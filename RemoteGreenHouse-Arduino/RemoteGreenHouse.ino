#include <Wire.h>
#include <Adafruit_BMP085.h>
#include <DHT.h>
#include <Adafruit_SSD1306.h>
#include <BH1750FVI.h>

// Pin declaration
#define dht_air_pin 5
#define relay1_pin 6
#define relay2_pin 7
#define relay3_pin 8
#define relay4_pin 9
#define mosfet_led_pin 10
#define mosfet_fan_pin 11

// function declaration
void updateLight();
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
void readTime();

// object setup
Adafruit_BMP085 barometer;                  //Barometer BMP180
Adafruit_SSD1306 display(4);                //Display 
DHT dht_air(dht_air_pin, DHT22);            //DHT22 Luftsensor an Pin D5
BH1750FVI lightSensor;                      //Lichtesensorobjekt

// parameter declaration
unsigned long curMillis;
unsigned long prevMillis;
unsigned long delayMillis;

long controlValues[5];     // State variables: 
                              //[0]: brightness of the LED Stripes          (0% - 100%)
                              //[1]: time when light turns on               [ms] since 00:00:00
                              //[2]: time when light turns off              [ms] since 00:00:00
                              //[3]: brightness when light turns on/off     [lux]
                              //[4]: light control mode                     [0/1/2/3]
                                // 4.0 : use time limits and brightness limits
                                // 4.1 : ignore time limits and use brightness limits
                                // 4.2 : use time limits and ignore brightness limits
                                // 4.3 : ignore both limits. Light is always on
                              
float sensorValues[5];    // Sensor values:
                              //[0]: Temperature    [°C]
                              //[1]: humidity       [g/kg]
                              //[2]: pressure       [Pa]
                              //[3]: moisture       [g/kg]
                              //[4]: brightness     [lux]

String  readString;         // read data of the serial input
String  limiterByte;        // limiter Byte for serial communication
long    currentTime;        // current daytime in millis      [ms]
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
  lightSensor.begin();                        //Starte lightSensor
  lightSensor.SetAddress(0x23);               //I²C Adresse des Sensors
  Serial.begin(9600);

  /* 
   * --------------------
   * ***Sensoren Setup***
   * --------------------
   */
  //Barometer Setup()
  barometer.begin();
  //lightSensor Setup()
  lightSensor.SetMode(Continuous_H_resolution_Mode);
  
  initializePins();
  initializeVariables();
  
  //Display Setup()
  display.setTextColor(WHITE);
  display.clearDisplay();
  displayStartMessage();
}

void loop() {  
  // take sensor inputs
  curMillis           = millis();
  sensorValues[0]     = dht_air.readTemperature();            // measure val_temperature
  sensorValues[1]     = getWaterContent();                    // measure val_humidity
  sensorValues[2]     = (barometer.readPressure()+1158)/100;  // measure val_pressure
  sensorValues[3]     = 0;                                    // until there is a moisture sensor
  sensorValues[4]     = lightSensor.GetLightIntensity();      // measure brightness
  
  while(Serial.available()){    
    readString = readComPort();
  }
  if(readString.length() > 0){
    int iCommand = readString[0];   // read the request
    readString.remove(0,1);         // remove iCommand from readString
    readTime();                     // read time from readString and remove it incl limiter Byte ;
    if(iCommand == 119){            // w[119] get request from android device
      String sSendString = "";
      for(int i = 0; i<(sizeof(sensorValues)/sizeof(long)); i++) {
        sSendString += String(sensorValues[i]) + ";";
      }
      Serial.println(sSendString);
    }
    else if(iCommand == 120){       //x[120] set request from android device
      int limiterPosition = 0;
      for(int i=0; i<(sizeof(controlValues)/sizeof(long)); i++){
        for(int j=0; j<=readString.length(); j++){
          if(readString[j] == 59){
            limiterPosition = j;
            goto fixpoint;
          }
        }
        fixpoint:
        String str = readString.substring(0,limiterPosition);
        controlValues[i] = str.toInt();
        readString.remove(0,limiterPosition+1);
        limiterPosition = 0;
      }
    }
    else if(iCommand == 121){     //y[121] get request for the controlValues
      String sSendString = "s";
      for(int i = 0; i<(sizeof(controlValues)/sizeof(long)); i++) {
        sSendString += String(controlValues[i]) + ";";
      }
      Serial.println(sSendString);
    }
    iCommand = 0;
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
  
  updateLight();
  updateTime();
}


// -------------------------------------
// ------------- Functions -------------
// -------------------------------------

void updateLight(){
  switch(controlValues[4]){
    case 0:
      if(currentTime >= controlValues[1] && currentTime <= controlValues[2]){
        if(sensorValues[4] <= 0.95*controlValues[3]){      
        //if(controlValues[3] = -1){
          //int i = 255*(controlValues[3] - sensorValues[4])/(controlValues[3]-10);        
          //setLEDlevel(i);
        //}else{
        setLEDlevel(controlValues[0]);
        //}
        }
        else if(sensorValues[4] > 1.05*controlValues[3]){      
          setLEDlevel(0);
        }
      }
      else{
        setLEDlevel(0);
      }
      break;
      
    case 1:
      if(sensorValues[4] <= 0.95*controlValues[3]){      
        //if(controlValues[3] = -1){
          //int i = 255*(controlValues[3] - sensorValues[4])/(controlValues[3]-10);        
          //setLEDlevel(i);
        //}else{
        setLEDlevel(controlValues[0]);
        //}
      }
      else if(sensorValues[4] > 1.05*controlValues[3]){      
        setLEDlevel(0);
      }
      break;
      
    case 2:
      if(currentTime >= controlValues[1] && currentTime <= controlValues[2]){
        setLEDlevel(controlValues[0]);
      }
      else{
        setLEDlevel(0);
      }
      break;
      
    case 3:
      setLEDlevel(controlValues[0]);
      break;
  }
}

void updateTime(){
  if(currentTime > 86400000){
    currentTime = 0;
  }
  else{
    currentTime = currentTime + millis() - milliTimer;
    milliTimer = millis();    
  }
}

void readTime(){
  currentTime = 0;
  for(int i=0; i<=readString.length(); i++){
    if(readString[i] == 59){
      currentTime = readString.substring(0,i).toInt();
      readString.remove(0,i+1);
      break;
    }
  }
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
  String printString = "Temp: " + String(sensorValues[0]) + " *C \n\nHum: "  + String(sensorValues[1]) + " g/kg";
  display.println(printString);
  display.display();
}

void displayPressBright(){
  display.clearDisplay();
  display.setTextSize(1);
  display.setCursor(0,5);
  String printString = "Bright: " + String(sensorValues[4]) + " lux \n\nPres: " + String(sensorValues[2]) + " hPa";
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
    delay(10);
    if (Serial.available() >0) {
      char c = Serial.read();
      readString += c;
    }
  }
  return readString;
}

String convert_ASCII_Code(int mASCII){
  String outString;
   if      (mASCII == 48) outString = "0";
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
  controlValues[0]          = 100;          // default brightnes
  controlValues[1]          =  8*3600000;   // default turn on time  8 am
  controlValues[2]          = 20*3600000;   // default turn off time 8 pm
  controlValues[3]          = 400;          // default brightneslimit
  controlValues[4]          = 3;            // default light mode (always on)
  for(int i = 0; i<(sizeof(sensorValues)/sizeof(long)); i++){
    sensorValues[i]        = 0;
  }  
  limiterByte               = ";";      //ASCII 59
  readString                = "";
  prevMillis                = millis();
  delayMillis               = 5000;
  milliTimer                = millis();
  currentTime               = 0;
}

void initializePins(){
  // Setting pin modes (mosfet pins are pwm pins)
  pinMode(relay1_pin,     OUTPUT);
  pinMode(relay2_pin,     OUTPUT);
  pinMode(relay3_pin,     OUTPUT);
  pinMode(relay4_pin,     OUTPUT);
  pinMode(dht_air_pin,    INPUT);

  // Set initial pin conditions
  digitalWrite(relay1_pin, LOW);
  digitalWrite(relay2_pin, LOW);
  digitalWrite(relay3_pin, LOW);
  digitalWrite(relay4_pin, LOW);
  analogWrite(mosfet_led_pin, 0);
  analogWrite(mosfet_fan_pin, 0);
}

