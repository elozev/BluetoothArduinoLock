#include <SoftwareSerial.h>
#include<LiquidCrystal.h>

//#include <mcrypt.h>
//#include <stdio.h>
//#include <stdlib.h>
//#include <string.h>
//#include <math.h>
//#include <stdint.h>
//#include <stdlib.h>


#define LED_PIN 13
#define YELLOW_LED_PIN 6
#define BUTTON_PIN 7

#define TX 11
#define RX 12
#define PERIOD_TIME 60000
#define PAIR_TIME 5000

SoftwareSerial BT(TX, RX);
// creates a "virtual" serial port/UART
// connect BT module TX to D10
// connect BT module RX to D11
// connect BT Vcc to 5V, GND to GND

String passwords[] = {
  "songswordswrongbymehillsheardtimed",
  "losehillwellupwillheoveron",
  "wonderbedelinorfamilysecuremet",
  "thefarattachmentdiscoveredcelebrateddecisivelysurroundedforand",
  "aroundreallyhisuseuneasylongerhimman",
  "ferrarsallspiritshisimagineeffectsamongstneither",
  "uptodenotingsubjectssensiblefeelingsitindulgeddirectly",
  "ownmarianneimprovedsociablenotout",
  "convincedresolvingextensivagreeableinitonasremainder",
  "occasionalprinciplesdiscretionitasheunpleasingboisterous"
};

String devices[20];
int devCounter = 0;

void setup()
{

  Serial.begin(115200);
  while (!Serial) {
    ; // wait for serial port to connect. Needed for native USB port only
  }

  // set digital pin to control as an output
  pinMode(LED_PIN, OUTPUT);

  // set the data rate for the SoftwareSerial port
  BT.begin(9600);

  // Send test message to other device
  sendMessageToBT("Hello from Arduino");

  pinMode(YELLOW_LED_PIN, OUTPUT);
  pinMode(BUTTON_PIN, INPUT);

  //  digitalWrite(YELLOW_LED_PIN, HIGH);

  delay(2000);
}

char a; // stores incoming character from other device
char prevA;
String command = "";

int period = 0;
unsigned long timeToNextPeriod = PERIOD_TIME;
unsigned long lastPeriodChange = 0;



bool isPasswordPending = false;
String passwordPending = "";

String deviceIdPending = "";

unsigned long pairStartTime;
bool isInPair = false;
bool isLogin = false;


void loop() {


  debounceButton(digitalRead(BUTTON_PIN));

  pairDevices();

  periodChecker();
  //  lcd.print(millis());
  if (BT.available()) {
    a = (BT.read());
    inputHandler(a);
  }
}

void pairDevices() {
  if (isInPair) {
    if ((millis() - pairStartTime) >= PAIR_TIME) {
      isInPair = false;
      digitalWrite(YELLOW_LED_PIN, LOW);
    }
  }
}

char ledCommand;

int buttonState;             // the current reading from the input pin
int lastButtonState = LOW;   // the previous reading from the input pin

unsigned long lastDebounceTime = 0;  // the last time the output pin was toggled
unsigned long debounceDelay = 50;

void debounceButton(int reading) {

  if (reading != lastButtonState) {
    lastDebounceTime = millis();
  }

  if ((millis() - lastDebounceTime) > debounceDelay) {
    if (reading != buttonState) {
      buttonState = reading;

      if (buttonState == HIGH) {
        callPair();
      }
    }
  }
  lastButtonState = reading;
}

void callPair() {
  sendMessageToBT("Pairing...");
  digitalWrite(YELLOW_LED_PIN, HIGH);
  isInPair = true;
  pairStartTime = millis();
}

void periodChecker() {
  unsigned long now = millis();
  if ((now - lastPeriodChange) >= PERIOD_TIME) {
    period++;
    if (period > 9)
      period = 0;
    lastPeriodChange = now;
  }
  timeToNextPeriod = PERIOD_TIME - (now - lastPeriodChange);
}

bool passwordChecker(String pass) {
  return passwords[period] == pass;
}

void inputHandler(char a) {
  Serial.println(a);

  if (!isPasswordPending && !isInPair && !isLogin) {
    switch (a) {
      case 'r':
        BT.println(period);
        // sendMessageToBT(period + "");
        BT.println(timeToNextPeriod);
        //   sendMessageToBT(timeToNextPeriod + "");
        break;
      case 'o':
        //        digitalWrite(LED_PIN, HIGH);
        //        BT.println("LED on");
        //        Serial.println(digitalRead(3));
        //        Serial.println(a);
        break;
      case 'f':
        //        digitalWrite(LED_PIN, LOW);
        //        BT.println("LED off");
        //        Serial.println(digitalRead(3));
        //        Serial.println(a);
        break;
      case 's':
        BT.println(digitalRead(LED_PIN));
        break;
      case 'p':
        break;
      case ':':
        if (prevA == 'p') {
          isPasswordPending = true;
          passwordPending = "";
          //          BT.println("Enter your password: ");
          sendMessageToBT("Enter your password: ");
        } else if (prevA == 'd') {
          if (isInPair) {
            //            BT.println("Enter device id:");
            sendMessageToBT("Enter your password: ");
          } else {
            //            BT.println("Press the button!");
            sendMessageToBT("Press the button!");
          }
        } else if (prevA == 'l') {
          isLogin = true;
        } else if (prevA == 'o') {
          isPasswordPending = true;
          passwordPending = "";
          ledCommand = 'o';
        } else if (prevA == 'f') {
          isPasswordPending = true;
          passwordPending = "";
          ledCommand = 'f';
        }
        break;
    }
    prevA = a;
  } else if (isPasswordPending) {
    if (a != '/') {
      passwordPending += a;
    } else {
      isPasswordPending = false;
      //BT.println(passwordPending);
      sendMessageToBT(passwordPending + "");
      bool check = passwordChecker(passwordPending);
      if (check) {
        sendMessageToBT("Access granted!");
        //        BT.println("Access granted!");
        if (ledCommand == 'o') {
          digitalWrite(LED_PIN, HIGH);
        } else if (ledCommand == 'f') {
          digitalWrite(LED_PIN, LOW);
        }
      } else {
        sendMessageToBT("Access denied!");
        //BT.println("Access denied!");
      }

      passwordPending = "";
    }
  } else if (isInPair) {
    if (a != '/') {
      deviceIdPending += a;
    } else {
      sendMessageToBT(deviceIdPending);
      //BT.println(deviceIdPending);
      addToDeviceList(deviceIdPending);
      deviceIdPending = "";
    }
  } else if (isLogin) {
    if (a != '/') {
      deviceIdPending += a;
    } else {
      if (checkLogin(deviceIdPending)) {

        char speriod[1];
        char stimeToNextPeriod[20];

        itoa(period, speriod, 1);
        itoa(period, stimeToNextPeriod, 20);

        String sp(speriod);
        String st(stimeToNextPeriod);

        String msg = "Dev registrated|";
        //        msg.concat(period);
        BT.print("somekey");
        BT.print('.');
        BT.print(period);
        BT.print('.');
        BT.print(timeToNextPeriod);
        BT.println('.');
        BT.println(passwords[period]);
        BT.flush();
        //            msg.concat('|');
        //            msg.concat(timeToNextPeriod);

        //sendMessageToBT(msg);
      } else {
        sendMessageToBT("Dev not registrated!");
      }
      isLogin = false;
      deviceIdPending = "";
    }
  }
}

bool checkLogin(String dev) {
  int i;
  for (i = 0; i < 20; i++) {
    if (devices[i] == dev) {
      return true;
    }
  }
  return false;
}

void addToDeviceList(String dev) {
  if (devCounter <= 20) {
    devices[devCounter] = dev;
    devCounter++;
    sendMessageToBT("Device added to security list!");
  } else {
    sendMessageToBT("Device security list full!");
  }
}

//void pass() {
//  uint8_t key[] = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
//  //String key2 = "hahaha";
//
//  char data[] = "0123";
//  aes256_enc_single(key, data);
//  Serial.print("encrypted:");
//  Serial.print(data);
//
//  BT.print("encrypted:");
//  BT.println(data);
//
//  char data2[] = "eRdYzLYQrbN98q9FE1x1pZcJzpGAVGdtDo5RlLqDh0k=";
//  aes256_dec_single(key, data);
//
//  Serial.print("decrypted:");
//  Serial.println(data2);
//
//  BT.print("decrypted:");
//  BT.println(data);
//}

void sendMessageToBT(String message) {
  BT.println(message);
  BT.flush();
}





