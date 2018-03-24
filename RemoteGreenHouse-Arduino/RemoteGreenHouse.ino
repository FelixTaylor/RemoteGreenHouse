#include <Wire.h>
#include <Adafruit_BMP085.h>
#include <DHT.h>
#include <Adafruit_SSD1306.h>
#include <BH1750FVI.h>

// Pin declaration
#define dht_terra_pin 4
#define dht_air_pin 5
#define relay1_pin 6
#define relay2_pin 7
#define relay3_pin 8
#define relay4_pin 9
#define mosfet_led_pin 10
#define mosfet_fan_pin 11

// function declaration
void updateTime();
void setLEDlevel(int val);
String readComPort();
String convert_ASCII_Code(int mASCII);
float getWaterContent();
void initializeVariables();
void initializePins();
void displayStartMessage();
void displayPressBright();
void displayTempHum();
void readTime(String str);

// object setup
Adafruit_BMP085 barometer;                  //Barometer BMP180
Adafruit_SSD1306 display(4);                //Display 
DHT dht_air(dht_air_pin, DHT22);            //DHT22 Luftsensor an Pin D5
DHT dht_terra(dht_terra_pin, DHT22);        //DHT22 Bodensensor an Pin D4
BH1750FVI lightSensor;                      //Lichtesensorobjekt

// parameter declaration
unsigned long curMillis;
unsigned long prevMillis;
unsigned long delayMillis;

boolean b_LED_state;        // State of the LED Stripes       (True: ON, False: OFF)
int     i_LED_level;        // brightness of the LED Stripes  (0% - 100%)
int     i_fan_level;        // state of the fan               (0: off, 1: partial load, 2: full load)
int     val_pressure;       // pressure value                 [Pa]
int     val_brightness;     // brightness value               [lux]
float   val_temperature;    // temperature value              [°C]
float   val_humidity;       // humidity value                 [%]
float   val_moisture;       // moisture value                 [%]
String  readString;         // read data of the serial input
//String  temperatureLimiter, humidityLimiter, pressureLimiter, moistureLimiter, brightnessLimiter, lightlevelLimiter, fanlevelLimiter;
String  limiterByte;        // limiter Byte for serial communication
long    lastTime;           // last time in millis            [ms]
long    milliTimer;         // millis since time has been synchronized

void setup() {
  /* 
   * -----------------------------------------
   * ***Serielle Schnittstellen und I2C Bus***
   * -----------------------------------------
   */
  Wire.begin(0x77);                           //Starte I²C des Barometers (0x77)
  display.begin(SSD1306_SWITCHCAPVCC, 0x3C);  //Starte I²C des Displays (0x3C)
  dht_air.begin();                            //Starte Luftsensor
  dht_terra.begin();                          //Starte Bodensensor
  lightSensor.begin();                        //Starte lightSensor
  lightSensor.SetAddress(0x23);               //I²C Adresse des Sensors
  Serial.begin(9600);

  /* 
   * --------------------
   * ***Sensoren Setup***
   * --------------------
   */
  //Barometer Setup()
  if (!barometer.begin()) {
    Serial.println("Could not find a valid BMP085 sensor, check wiring!");
  }
  
  //lightSensor Setup()
  lightSensor.SetMode(Continuous_H_resolution_Mode);
  //Display Setup()
  display.setTextColor(WHITE);
  display.clearDisplay();
  displayStartMessage();

  //Pinmode Setup and initial pin conditions
  initializePins();

  // Initialize the variables
  initializeVariables();

}

void loop() {
  // take sensor inputs
  curMillis         = millis();
  val_temperature   = dht_air.readTemperature();            // measure val_temperature
  val_humidity      = getWaterContent();                    // measure val_humidity
  val_pressure      = (barometer.readPressure()+1158)/100;  // measure val_pressure
  val_moisture      = 0;                                    // until there is a moisture sensor
  val_brightness    = lightSensor.GetLightIntensity();      // measure brightness
  
  // Read SerialData if serial is available
  while(Serial.available()){    
    readString = readComPort();
  }
  // evaluate incoming data
  if(readString.length() > 0){
    //    w[199]: send sensor values
    //    x[120]: set LED state
    //    y[121]: set fan state
    //    z[122]: set pump state
    int iType = readString[0];
    //w[119] send sensor values
    if(iType == 119){
      readTime(readString); 
      String sSendString = val_temperature + limiterByte + val_humidity + limiterByte + val_pressure + limiterByte + val_moisture + limiterByte + val_brightness + limiterByte + i_LED_level + limiterByte + i_fan_level + limiterByte;
      Serial.println(sSendString);
      sSendString = "";
    }
    //x[120] set LED state
    else if(iType == 120){
      readTime(readString); 
      i_LED_level = (convert_ASCII_Code(readString[1])+convert_ASCII_Code(readString[2])+convert_ASCII_Code(readString[3])).toInt();
      if(i_LED_level > 100)i_LED_level = 100;
      setLEDlevel(i_LED_level);
    }
    //y[121] set fan state
    else if(iType == 121){
      readTime(readString);      
    }
    //z[122] set pump state
    else if(iType == 122){
      readTime(readString);
      if(readString[1] == 1){             // turn on pump  
        digitalWrite(relay1_pin, HIGH);
      }
      else if(readString[1] == 2){
        digitalWrite(relay1_pin, LOW);    // turn off pump
      }
    }
    iType = 0;
    readString = "";
  }
/*
 * --Debug--
  display.clearDisplay();
  display.setTextSize(1);
  display.setCursor(0,5);
  int hours = lastTime/3600000;
  int minut = (lastTime-hours*3600000)/60000;
  String printString = "Time: " + String(hours) + ":" + String(minut);
  display.println(printString);
  prevMillis = millis();
  display.display();*/
  
  // display the current sensor values
  if(curMillis - prevMillis < delayMillis){
    displayPressBright();
  }
  else if(curMillis - prevMillis > delayMillis && curMillis - prevMillis < 2*delayMillis){    
    displayTempHum();
  }
  else{
    prevMillis = millis();
  }
  updateTime();
}


// -------------------------------------
// ------------- Functions -------------
// -------------------------------------

void updateTime(){
  if(lastTime > 86400000){
    lastTime = 0;
  }
  else{
    lastTime = lastTime + millis() - milliTimer;
    milliTimer = millis();    
  }
}

void readTime(String str){
  lastTime = 0;
  int pos = str.length()-1;
  int secs = convert_ASCII_Code(str[pos-1]).toInt()*10 + convert_ASCII_Code(str[pos]).toInt();
  int mins = convert_ASCII_Code(str[pos-3]).toInt()*10 + convert_ASCII_Code(str[pos-2]).toInt();
  int hrs  = convert_ASCII_Code(str[pos-5]).toInt()*10 + convert_ASCII_Code(str[pos-4]).toInt();
  /*
  int secs = (convert_ASCII_Code(str[pos-4])+convert_ASCII_Code(str[pos-3])).toInt();
  int mins = (convert_ASCII_Code(str[pos-6])+convert_ASCII_Code(str[pos-5])).toInt();
  int hrs  = (convert_ASCII_Code(str[pos-8])+convert_ASCII_Code(str[pos-7])).toInt();
  */
  lastTime = secs*1000 + mins*60000 + hrs*3600000;
}

void setLEDlevel(int val){
  analogWrite(mosfet_led_pin,val*2.55);
}

void setPump(int mTime){
  long daytime = 86400000;
}

void displayTempHum(){
  display.clearDisplay();
  display.setTextSize(1);
  display.setCursor(0,5);
  String printString = "Temp: " + String(val_temperature) + " *C \n\nHum: "  + String(val_humidity) + " g/kg";
  display.println(printString);
  display.display();
}

void displayPressBright(){
  display.clearDisplay();
  display.setTextSize(1);
  display.setCursor(0,5);
  String printString = "Bright: " + String(val_brightness) + " lux \n\nPres: " + String(val_pressure) + " hPa";
  display.println(printString);
  display.display();
}

void displayStartMessage(){
  display.setCursor(0,0);
  display.setTextSize(2);
  display.println("Remoted \nGreenHouse");
  display.display();
  delay(5000);
}

float getWaterContent(){
  float  pws = 6.11657*exp(17.2799-(4102.99/(dht_air.readTemperature()+237.431)));
  return 1000*0.622*pws/(1013.25*100/dht_air.readHumidity() - pws);
}

String readComPort(){
  while (Serial.available()) {
    if (Serial.available() >0) {
      char c = Serial.read();
      readString += c;
    }
  }
  return readString;
}

String convert_ASCII_Code(int mASCII){
  String outString;
   if (mASCII == 48) outString = "0";
   else if (mASCII == 49) outString = "1";
   else if (mASCII == 50) outString = "2";
   else if (mASCII == 51) outString = "3";
   else if (mASCII == 52) outString = "4";
   else if (mASCII == 53) outString = "5";
   else if (mASCII == 54) outString = "6";
   else if (mASCII == 55) outString = "7";
   else if (mASCII == 56) outString = "8";
   else if (mASCII == 57) outString = "9";
  return outString;
}

void initializeVariables(){
  b_LED_state               = false;
  i_LED_level               = 0;
  i_fan_level               = 0;
  val_brightness            = 0;
  val_pressure              = 0;
  val_temperature           = 0; 
  val_humidity              = 0;
  val_moisture              = 0;
  limiterByte               = ";";      //ASCII 59
  readString                = "";
  prevMillis                = millis();
  delayMillis               = 5000;
  milliTimer                = millis();
  lastTime                  = 0;
}

void initializePins(){
  // Setting pin modes (mosfet pins are pwm pins)
  pinMode(relay1_pin,     OUTPUT);
  pinMode(relay2_pin,     OUTPUT);
  pinMode(relay3_pin,     OUTPUT);
  pinMode(relay4_pin,     OUTPUT);
  pinMode(dht_terra_pin,  INPUT);
  pinMode(dht_air_pin,    INPUT);

  // Set initial pin conditions
  digitalWrite(relay1_pin, LOW);
  digitalWrite(relay2_pin, LOW);
  digitalWrite(relay3_pin, LOW);
  digitalWrite(relay4_pin, LOW);
  analogWrite(mosfet_led_pin, 0);
  analogWrite(mosfet_fan_pin, 0);
}

