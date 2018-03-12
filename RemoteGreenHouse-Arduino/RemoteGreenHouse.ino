#include <SPI.h>
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
void setLEDlevel(int val);
int getPressure();
double getTemperature();
float getHumidity();
uint16_t getLightIntensity();
String readComPort();
String convert_ASCII_Code(int mASCII);
double getWaterContent();
void initializeVariables();
void initializePins();
void displayStartMessage();
void displayPressBright();
void displayTempHum();

// object setup
Adafruit_BMP085 barometer;                  //Barometer BMP180
Adafruit_SSD1306 display(4);                //Display 
DHT dht_air(DHT22, dht_air_pin);            //DHT22 Luftsensor an Pin D5
DHT dht_terra(DHT22, dht_terra_pin);        //DHT22 Bodensensor an Pin D4
BH1750FVI lightSensor;                      //Lichtesensorobjekt

// parameter declaration
unsigned long curMillis;
unsigned long prevMillis;
unsigned long delayMillis;

boolean b_LED_state;        // State of the LED Stripes       (True: ON, False: OFF)
int     i_LED_level;        // brightness of the LED Stripes  (0% - 100%)
int     i_fan_level;        // state of the fan               (0: off, 1: partial load, 2: full load)
int     val_pressure;       // pressure value                 [Pa]
long    val_brightness;     // brightness value               [lux]
double  val_temperature;    // temperature value              [°C]
float   val_humidity;       // humidity value                 [%]
float   val_moisture;       // moisture value                 [%]
String  readString;         // read data of the serial input
String  temperatureLimiter, humidityLimiter, pressureLimiter, moistureLimiter, brightnessLimiter, lightlevelLimiter, fanlevelLimiter;

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
  while (1) {}
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
  val_temperature   = getTemperature();             // measure val_temperature
  val_humidity      = getWaterContent();            // measure val_humidity
  val_pressure      = getPressure();                // measure val_pressure
  val_moisture      = 0;                            // until there is a moisture sensor
  val_brightness    = getLightIntensity();          // measure brightness
  
  // Read SerialData if serial is available
  readString    = readComPort();

  // evaluate incoming data
  if(readString.length() > 0){
    // divide the readString into a Typ
    //    a[97] : Controlling the LED
    //    h[104]: send sensor values to the application
    int iType = readString[0];
    if(iType == 97){                              // Empfangsdaten: a (ASCII: 97)  LED AN/AUS
      i_LED_level = (convert_ASCII_Code(readString[1])+convert_ASCII_Code(readString[2])+convert_ASCII_Code(readString[3])).toInt();
      if(i_LED_level > 100)i_LED_level = 100;
      setLEDlevel(i_LED_level);
    }
    else if(iType == 104){      
      //String sSendString = brightnessLimiter + val_brightness + humidityLimiter + val_humidity + pressureLimiter + val_pressure + temperatureLimiter + val_temperature + lightlevelLimiter + i_LED_level;
      String sSendString = temperatureLimiter + val_temperature + humidityLimiter + val_humidity + pressureLimiter + val_pressure + moistureLimiter + val_moisture + brightnessLimiter + val_brightness + lightlevelLimiter + i_LED_level + fanlevelLimiter + i_fan_level;
      Serial.println(sSendString);
      sSendString = "";
    }
    iType = 0;
    readString = "";
  }

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
}


// -------------------------------------
// ------------- Functions -------------
// -------------------------------------

void setLEDlevel(int val){
  double lightValue = val*2.55;
  analogWrite(mosfet_led_pin,lightValue);
}

int getPressure(){
  return barometer.readPressure()/100;
}

double getTemperature(){
  return barometer.readTemperature();
}

float getHumidity(){
  return dht_air.readHumidity();
}

uint16_t getLightIntensity(){
  return lightSensor.GetLightIntensity();
}

void displayTempHum(){
  display.clearDisplay();
  display.setTextSize(1);
  display.setCursor(0,5);
  display.print("Temp: ");
  display.print(val_temperature);
  display.println(" *C");
  display.println("");
  display.print("Hum: ");
  display.print(val_humidity);
  display.print(" g/kg");
  display.display();
}

void displayPressBright(){
  display.clearDisplay();
  display.setTextSize(1);
  display.setCursor(0,5);
  display.print("Bright: ");
  display.print(val_brightness);
  display.println(" lux");
  display.println("");
  display.print("Pres: ");
  display.print(val_pressure);
  display.print(" hPa");
  display.display();
}

void displayStartMessage(){
  display.setCursor(0,0);
  display.setTextSize(2);
  display.println("Remoted");
  display.println("GreenHouse");
  display.display();
  delay(5000);
}

double getWaterContent(){
  double  pws = 6.11657*exp(17.2799-(4102.99/(dht_air.readTemperature()+237.431)));
  double  mw = 1000*0.622*pws/(1013.25*100/dht_air.readHumidity() - pws);
  return mw;
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
  temperatureLimiter        = "t";
  humidityLimiter           = "h";
  pressureLimiter           = "p";
  moistureLimiter           = "m";
  brightnessLimiter         = "b"; 
  lightlevelLimiter         = "l";
  fanlevelLimiter           = "f";
  readString                = "";
  prevMillis                = millis();
  delayMillis               = 5000;
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

