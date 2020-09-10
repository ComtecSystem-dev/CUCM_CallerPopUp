## What is this?
This example program displays the caller information registered in the phonebook.
        - Display image/text caller information depending on phone model.
        - Auto-hide caller information when a call is dropped.
        - Answer a call.
        - Support 

## Development environment.
	CUCM 11.5
	Cisco JTAPI
	Cisco IP Phone Services Software Development Kit
	Eclipse
	Spring boot

## Used to
you should get sample code in "source" directory

#### Setting
you change the setting to use it.

path : /source/src/main/resource/application.properties

	server.port=<APP Lisen Port>
	ipps.cucm.providerString=<CUCM IP1>,<CUCM IP2>;login=<CUCM APP ID>;passwd=<CUCM APP PWD>;appinfo=callerPopup
	ipps.server.url=http://<ServerIP>:<ServerPort>
	ipps.path.popup = /popup
	ipps.path.images = /images
	ipps.path.answer = /answer
	logging.level.com.comtec.ipps = DEBUG


## License
This project is licensed under the Apache License 2.0 - see the LICENSE.md file for details.
