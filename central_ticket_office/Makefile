default:
	clear
	scalac -d out -unchecked  -classpath ./out/ src/PrintsSerializer.scala
	scalac -d out -unchecked -feature -classpath ./out/:../lib/yami4.jar:../lib/json-smart-1.1.1.jar src/JSONScala.scala
	scalac -d out -unchecked  -classpath ./out/ src/Ticket.scala
	scalac -d out -unchecked  -classpath ./out/ src/Route.scala
	scalac -d out -unchecked  -classpath ./out/:../lib/yami4.jar src/Messages.scala
	scalac -d out -unchecked  -classpath ./out/:../lib/yami4.jar src/BookingManager.scala
	scalac -d out -unchecked  -classpath ./out/:../lib/yami4.jar src/PathResolver.scala
	scalac -d out -unchecked  -classpath ./out/:../lib/yami4.jar src/SynchRequestsHandler.scala
	scalac -d out -unchecked  -classpath ./out/:../lib/yami4.jar src/MessagesReceiver.scala
	scalac -d out -unchecked  -classpath ./out/:../lib/yami4.jar src/TicketOffice.scala
fast: 
	clear
	fsc -d out -unchecked  -classpath ./out/ src/PrintsSerializer.scala
	fsc -d out -unchecked -feature -classpath ./out/:../lib/yami4.jar:../lib/json-smart-1.1.1.jar src/JSONScala.scala
	fsc -d out -unchecked  -classpath ./out/ src/Ticket.scala
	fsc -d out -unchecked  -classpath ./out/ src/Route.scala
	fsc -d out -unchecked  -classpath ./out/:../lib/yami4.jar src/Messages.scala
	fsc -d out -unchecked  -classpath ./out/:../lib/yami4.jar src/BookingManager.scala
	fsc -d out -unchecked  -classpath ./out/:../lib/yami4.jar src/PathResolver.scala
	fsc -d out -unchecked  -classpath ./out/:../lib/yami4.jar src/SynchRequestsHandler.scala
	fsc -d out -unchecked  -classpath ./out/:../lib/yami4.jar src/MessagesReceiver.scala
	fsc -d out -unchecked  -classpath ./out/:../lib/yami4.jar src/TicketOffice.scala
clean:
	rm -f out/*.class
