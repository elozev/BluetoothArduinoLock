#include <SoftwareSerial.h>
#include<LiquidCrystal.h>
#include <AESLib.h>

#define LED_PIN 13
#define TX 11
#define RX 12
#define PERIOD_TIME 60000

SoftwareSerial BT(TX, RX);
// creates a "virtual" serial port/UART
// connect BT module TX to D10
// connect BT module RX to D11
// connect BT Vcc to 5V, GND to GND

LiquidCrystal lcd(9, 8, 5, 4, 3, 2);


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
  BT.println("Hello from Arduino");


  lcd.begin(16, 2);
  lcd.print("Hello Android");

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


void loop() {

  periodChecker();
  //  lcd.print(millis());
  if (BT.available()) {
    a = (BT.read());
    inputHandler(a);
  }
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
  
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print(period);
  lcd.setCursor(0, 1);
  lcd.print(millis() / 1000);
}

void passwordChecker(String pass) {
  if (pass == passwords[period]) {
    BT.println("Access granted");
  } else {
    BT.println("Access denied");
  }
}

void inputHandler(char a) {
  Serial.println(a);

  if (!isPasswordPending) {
    switch (a) {
      case 'r':
        BT.println(period);
        BT.println(timeToNextPeriod);
        break;
      case 'o':
        digitalWrite(LED_PIN, HIGH);
        BT.println("LED on");
        Serial.println(digitalRead(3));
        Serial.println(a);
        break;
      case 'f':
        digitalWrite(LED_PIN, LOW);
        BT.println("LED off");
        Serial.println(digitalRead(3));
        Serial.println(a);
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
          BT.println("Enter your password: ");
          //            lcd.clear();
          //            lcd.setCursor(0, 0);
          //            lcd.println("Enter pass:");
        }
        break;
    }
    prevA = a;
  } else {
    if (a != '/') {
      passwordPending += a;
      //        lcd.clear();
      //        lcd.setCursor(0, 1);
      //        lcd.println(passwordPending);
    } else {
      isPasswordPending = false;
      BT.println(passwordPending);
      passwordChecker(passwordPending);
      passwordPending = "";
    }
  }
}

void pass() {
  uint8_t key[] = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
  //String key2 = "hahaha";

  char data[] = "0123";
  aes256_enc_single(key, data);
  Serial.print("encrypted:");
  Serial.print(data);

  BT.print("encrypted:");
  BT.println(data);

  char data2[] = "eRdYzLYQrbN98q9FE1x1pZcJzpGAVGdtDo5RlLqDh0k=";
  aes256_dec_single(key, data);

  lcd.clear();
  lcd.print(data);


  Serial.print("decrypted:");
  Serial.println(data2);

  BT.print("decrypted:");
  BT.println(data);
  lcd.setCursor(0, 1);
  lcd.print(data);


}





