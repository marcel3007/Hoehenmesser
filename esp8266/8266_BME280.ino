#include <ESP8266WiFi.h>
#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_BME280.h>
#include <FirebaseArduino.h>

Adafruit_BME280 bme; // I2C

#define WIFI_SSID "yourWlanSSID"
#define WIFI_PASSWORD "yourPassword"

#define FIREBASE_HOST "link-to-firebase-host"
#define FIREBASE_AUTH "firebaseAuthKey"

static const int UPDATE_INTERVAL = 60000;

float humidity;
float temperature;
int pressure;

void setup() {
  Serial.begin(115200);

  delay(10);
    
  Wire.begin(D3, D4);
  Wire.setClock(100000);
 
  connectWiFi();

  if (!bme.begin(0x76)) {
    Serial.println("Could not find a valid BME280 sensor, check wiring!");
    while (1);
  }
  
  Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);

}

void loop() {

  humidity = bme.readHumidity();
  temperature = bme.readTemperature();
  pressure = bme.readPressure();
  
  setInt("htw/sensor/pressure", pressure);
  setFloat("htw/sensor/humidity", humidity);
  setFloat("htw/sensor/temperature", temperature);
  
  printSensorData();
 
  delay(UPDATE_INTERVAL); 

}

void setFloat(String path, float value){
  Firebase.setFloat(path, value);
  if (Firebase.failed()) { 
    Serial.print("Firebase get failed: "); 
    Serial.println(Firebase.error()); 
  }
}
void setInt(String path, int value){
  Firebase.setInt(path, value);
  if (Firebase.failed()) { 
    Serial.print("Firebase get failed: "); 
    Serial.println(Firebase.error()); 
  }
}

void printSensorData(){
  
  Serial.print("Pressure ");
  Serial.print(pressure/100.0);
  Serial.println(" hPa");
  
  Serial.print("Temperature ");
  Serial.print(temperature);
  Serial.println(" *C");
  
  Serial.print("Humidity ");
  Serial.print(humidity);
  Serial.println(" %");

}

void connectWiFi(){

  Serial.println();
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(WIFI_SSID);
  
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  
  Serial.println("");
  Serial.println("WiFi connected");  
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
}
