### NFC Replay Utility

The NFC Replay tool allows a PN 532 module to reenact a NFC transaction from a
raw bug report. Currently, the tool is capable of replaying polling loop transactions
and APDU exchanges. Once the transaction has been replayed, a test can
optionally be generated based on the interaction between the module and
emulator.

The detailed design for this feature can be found at go/nfc-replay-utility-dd.

### Using the tool

#### Generating and replaying a test

1\. Obtain a bug report from the device. You will need to locate the raw bug
report file (which will likely have the title "bugreport-...txt") and keep
track of its path.

2\. Connect the PN532 module via a serial port.

3\. To replay the transaction, substitute the path of the bug report file and
the serial port that the PN 532 module is using.

```
python3 nfcreplay.py -f $BUG_REPORT_FILE -p $READER_PATH
```

Alternatively, to replay a specific section of the bug report, additional
arguments should be added to denote the desired start and end time frame of the
transaction. For instance:

```
python3 nfcreplay.py -f $BUG_REPORT_FILE -p $READER_PATH --start "2024-07-17 12:00:00" --end "2024-07-17 15:00:00"
```

Information about the transaction will be printed out to console, including a
list of all polling loop and APDU exchanges that took place.

5\. To generate and run a test from the bug report, use the command:
```
python3 nfcreplay.py -f $BUG_REPORT_FILE -p $READER_PATH --generate_and_replay_test
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

To use the emulator app outside of a generated test, perform the following steps:

1\. To prepare a bug report to be replayed with the app:

```
python3 nfcreplay.py -f $BUG_REPORT_FILE --parse_only
```

The script will produce the name of the parsed log, which will be located within
the folder emulatorapp/parsed_files. Save the name for Step 3.

2\. Build and install the emulator app.

```
mma EmulatorApduAppNonTest
adb install -r -g $PATH_TO_EMULATOR_APP_APK

```

3\. Start the activity. Make sure that $$PARSED_FILE is the name of
the file, rather than its path. It is assumed that this file is located within
emulatorapp/parsed_files, where it was originally created.

```
adb shell am start -n com.android.nfc.emulatorapduapp/.MainActivity --es "snoop_file" "$PARSED_FILE"
```

When you are ready to start the transaction, press the "Start Host APDU Service"
button.

4\. To replay the transaction with the PN532 module, follow the steps above to
generate and replay a test case, though you should make sure to append the flag
`--replay_with_app` to the end of each command.

When the transaction is replayed, you should be able to see a list of APDU
commands and responses received and sent by the Host APDU service displayed on
the emulator app. Additionally, the replay script will output similar
information.