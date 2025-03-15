### NFC Replay Utility

The NFC Replay tool allows a PN 532 module to reenact a NFC transaction from a
snoop log. Currently, the tool is capable of replaying polling loop transactions
and APDU exchanges. Once the transaction has been replayed, a test can
optionally be generated based on the interaction between the module and
emulator.

The detailed design for this feature can be found at go/nfc-replay-utility-dd.

### Using the tool

#### Generating and replaying a test

1\. Obtain a snoop log from the device (see instructions below for how to do this).

2\. Connect the PN532 module via a serial port.

3\. To replay the transaction, substitute the name of the snoop file and the
serial port that the PN 532 module is using.

```
python3 nfcreplay.py -f $SNOOP_FILE -p $READER_PATH
```

Alternatively, to replay a specific section of the snoop log, additional
arguments should be added to denote the desired start and end time frame of the
transaction. For instance:

```
python3 nfcreplay.py -f $SNOOP_FILE -p $READER_PATH --start "2024-07-17 12:00:00" --end "2024-07-17 15:00:00"
```

Information about the transaction will be printed out to console, including a
list of all polling loop and APDU exchanges that took place.

5\. To generate and run a test from the snoop log, use the command:
```
python3 nfcreplay.py -f $SNOOP_FILE -p $READER_PATH --generate_and_replay_test
```

A Python file will be created, representing the test, along with a JSON file
that contains all information pertaining to APDUs transacted.

#### Using the Emulator App

The generated test will always involve the installation of the emulator app
(located at src/com/android/nfc/emulatorapp/) onto the emulator. The app handles
APDU transactions in cases where the replayed transaction involves a third party
app that the emulator does not access to. To guarantee that the emulator app is
able to handle the transaction, all AIDs sent in the original transaction will
be replaced with AIDs that the app is registered to handle (the specific values
are located in @xml/aids).

When the transaction is replayed, you should be able to see a list of APDU
commands and responses received and sent by the Host APDU service displayed on
the emulator app.

### Creating a Snoop Log

To create a snoop log from your Android device, you should first go to Developer
Options in Settings to make sure that "NFC NCI unfiltered log" is enabled. This
will ensure that the data packets sent during NFC transactions are not truncated
in the snoop log.

After the NFC transaction is complete, enter the command `adb shell dumpsys
nfc`. This will output the snoop log, which will begin with the line `---
BEGIN:NFCSNOOP_VS_LOG_SUMMARY` and end with the line `---
END:NFCSNOOP_VS_LOG_SUMMARY ---`. Copy the snoop log into a text file, and make
sure to include both the start and end lines.