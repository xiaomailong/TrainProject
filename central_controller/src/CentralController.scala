import scala.actors._
import com.inspirel.yami._
import com.inspirel.yami.OutgoingMessage.MessageState._
import scala.collection.mutable.ListBuffer

case class Event(description:String) 
case class Stop()
case class DistributedStop()
case class Continue()
case class NodeTerminated(nodeName : String)
case class Start()
case class InitController(address:String)


trait Response
case class Error() extends Response
case class Ok(map:Map[String,String]) extends Response

class Traveler(val id:String,val name:String,val surname:String) {
	
	def print {
		println("  id      = " + id)
		println("  name    = " + name)
		println("  surname = " + surname)
		println("--------------------")
	}
}

object Controller extends Actor {
	
	/**
	 * For each Train, contains the list of Travelers.
	 */
	var trainsOccupation : Map[String,ListBuffer[Traveler]] = Map()
	
	
	var segmentsTrain : Map[String,ListBuffer[String]] = Map()
	
	
	var stationsTrains : Map[String,ListBuffer[(String,String)]] = Map()
	
	val publisherValue 	= new ValuePublisher
	val publisherAgent 	= new Agent
	val senderAgent 	= new Agent
	
	/**
	 * Nodes asked to terminate
	 */
	var askedNodes = List[String]()
	
	/**
	 * Terminated Nodes
	 */
	var	terminatedNodes = List[String]()
	
	
	/**
	 * Loads the json file containg the location of regional ticket offices
	 *
	 * @return A list of couples (name,address)
	 */ 
	private def getMap(json : String) : Map[String,String] = {
		var res : Map[String,String] = Map()
		JSON.parseJSON(json).nodes.foreach(node => {res += node.name.toString -> node.address.toString})
		
		res.keys.foreach(k => println(k + "," + res(k)))
		
		res	
	}
	
	def sendMessage(address:String,destObject:String,service:String,params:Map[String,String]) : Response = {
		var result 	= Map[String,String]()
		var ok 		= false
		try {
			val p = new Parameters
			
			if (params != null) params.keys.foreach(k => p.setString(k,params(k)))
			
			val message = senderAgent.send(address,destObject,service,p)
			
			message.waitForCompletion()
			
			message.getState match {
				case REPLIED 	=> {
					ViewAgent ! Write("Message REPLIED from remote agent [" + destObject + "] service [" +service+ "] at " + address)
					val replyIterator = message.getReply.iterator
					while(replyIterator.hasNext) {
						val next = replyIterator.next()
						result += next.name -> next.getString()
					}
					ok = true
				}
				case REJECTED 	=> ViewAgent ! Write("ERROR: Message REJECTED from remote agent [" + destObject + "] service [" +service+ "] at " + address)
				case ABANDONED 	=> ViewAgent ! Write("ERROR: Message ABANDONED from remote agent [" + destObject + "] service [" +service+ "] at " + address)
				case _ 			=> 
			}
			
			
			
		} catch {
			case e : Throwable => Write("ERROR: Can not reach remote agent [" + destObject + "] service [" +service+ "] at " + address)
		}
		if (ok) Ok(result)
		else Error()
	}
	
	def getNodesList() : Map[String,String] = {
		
		sendMessage(ControllerMain.NAME_SERVER_ADDRESS,"name_server","list", Map()) match {
			case Ok(map) => map get ("response") match {
				case Some(s) => s match {
					case "OK" => getMap(map("list"))
					case _ 	 => Map()
				}
				case None 	=> Map()
			}
			case Error() => Map()
		}
	}
	
	def startTicketOffice() {
		sendMessage(ControllerMain.CENTRAL_TICKET_OFFICE_ADDRESS,"central_ticket_server","start",Map()) 
	}
	
	def startNodes() {
		
		val nodesList = getNodesList()
		if (nodesList.size == 0) return 
		nodesList.keys.foreach(node => {
			ViewAgent ! Write("Sending to " + node +" at " + nodesList(node))
			sendMessage(nodesList(node),"message_handler","start_simulation",Map())
		
		})
	}
	
	def sendMarker() : Boolean = {
		
		sendMessage(ControllerMain.CENTRAL_TICKET_OFFICE_ADDRESS,"central_ticket_server","marker",Map()) match {
			case Ok(map) => true
			case Error() => false
		}

	}
	
	def sendMarkerNameServer() {
		sendMessage(ControllerMain.NAME_SERVER_ADDRESS,"name_server","terminate",Map())
	}

	
	def printSegments() {
		println("** Segments Status ")
		segmentsTrain.keys.foreach(segment => {
			print("    Segment  [" + segment+"] :  | ")
			segmentsTrain(segment).foreach(train => print(train + " | "))
			println
		})
	}

	def printTrainsOccupation() {
		println("** Trains ")	
		trainsOccupation.keys.foreach(tr => {
			print("    Train ["+tr+"] :  | ")
			trainsOccupation(tr).foreach(traveler => print(traveler.id+" | "))
			println
		})
	}
	
	def printStationsTrains() {
		println("** Stations")
		stationsTrains.keys.foreach(station => {
			print("    Station [" + station + "] :  | ") 
			stationsTrains(station).foreach(c => print("(" + c._1 + "," + c._2 + ") | "))
			println
		})
	}

	def controllerLoop() {
		react {
			case Event(d) => {
				
				// Publish the Event
				val content = new Parameters
				//println ("Publishing event " + d)
				content.setString("event",d)
				publisherValue.publish(content)
				
				// Parse and memorize the Event data
				val event = JSON.parseJSON(d)
				event.apply("type").toString match {
					case "traveler_event" => {
						if (event.action.toString == "leave") {
							
							if (!trainsOccupation.contains(event.train_id.toString)) {
								trainsOccupation += event.train_id.toString -> ListBuffer[Traveler]()
							}
							trainsOccupation(event.train_id.toString) += 
								new Traveler(event.traveler_id.toString,event.name.toString,event.surname.toString)
							printTrainsOccupation()
						}
						if (event.action.toString == "enter") {
							
							if (trainsOccupation.contains(event.train_id.toString)) {
								var toRemove : Traveler = null;
								trainsOccupation(event.train_id.toString).foreach(el => {
									if (el.id == event.traveler_id.toString) toRemove = el
								})
								trainsOccupation(event.train_id.toString) -= toRemove
								printTrainsOccupation()
							}
						}
					}
					case "train_left" => {
						
						if (!segmentsTrain.contains(event.segment.toString)) {
							segmentsTrain += event.segment.toString -> ListBuffer()
						}
						segmentsTrain(event.segment.toString) += event.train_id.toString
						printSegments()
						if (stationsTrains.contains(event.station.toString)) {
							var toRemove : (String,String) = null
							stationsTrains(event.station.toString).foreach(train => {
								if (train._1 == event.train_id.toString) {
									toRemove = train
								}
							})
							stationsTrains(event.station.toString) -= toRemove
							printStationsTrains()
						}
					}
					case "train_arrived" => {
						if (segmentsTrain.contains(event.segment.toString)) {
							segmentsTrain(event.segment.toString) -= event.train_id.toString
							printSegments()							
						}
						if (!stationsTrains.contains(event.station.toString)) {
							stationsTrains += event.station.toString -> ListBuffer()
						}
						stationsTrains(event.station.toString) += ((event.train_id.toString,event.platform.toString))
						printStationsTrains()
					}
					case _ => 
				}
				
				controllerLoop
			}
			
			case DistributedStop() => {
				ViewAgent ! Write("Requested Distributed Termination")
				
				sendMarker match {
					case true => // do nothing
					case false => ViewAgent ! Write("ERROR: Central Ticket Office did not receive the Marker!")
						
				}
				
				controllerLoop()
			}
			
			case Continue() => {
			
				try {
					getNodesList match {
						case map : Map[_,_] => {
							if (map == null || map.size == 0){
								ViewAgent ! Write("WARNING: No Nodes to terminate!")
								ViewAgent ! Write("Sending the second Marker to Central Ticket Office...")
								if (sendMarker) {
									ViewAgent ! Write("Central Controller can be now termianted!")
									this ! Stop()
								}
							}else {
								val agent = new Agent
								map.keys.foreach( k => {
									val message : OutgoingMessage = agent.send(
										map(k),
										"message_handler", 
										"terminate", 
										new Parameters)
									message.waitForCompletion
									message.getState match {
										case OutgoingMessage.MessageState.REPLIED => {
											ViewAgent ! Write("Node "+k+" Received termination request.")
											// Add the node to asked list
											askedNodes ::= k
										}
										case OutgoingMessage.MessageState.REJECTED => 
											ViewAgent ! Write("ERROR: The message has been rejected by node "+ k +
													" : " +	message.getExceptionMsg)
										case _ => 
											ViewAgent ! Write("ERROR: The message has been abandoned by node " + k)
									}
								})
								agent.close
							}
						}
						case null => 
					}
					controllerLoop()
				} catch {
					case e : com.inspirel.yami.YAMIIOException => {
						println("ERRORE: Connessione rifiutata all'indirizzo " + ControllerMain.NAME_SERVER_ADDRESS)
						controllerLoop()
					}
				}
			}
			
			case NodeTerminated(nodeName) => {
				ViewAgent ! Write("Node "+nodeName+" confirmed Termination")
				askedNodes = askedNodes.filter(_ != nodeName)
				if (askedNodes.size == 0) {
					// All nodes terminated, so we can send another Marker to Central Ticket Office to 
					// Shut it down!
					if(sendMarker) {
						sendMarkerNameServer()
						ViewAgent ! Write("Central Controller can be now termianted!")
						this ! Stop()
					}
				}else{
					// If there are > 0 nodes to wait, loop again.
					controllerLoop()
				}
			}
			
			case Stop() => {
				ViewAgent ! DisableButtons()
				ViewAgent ! Write("Close the Window to quit central controller")
				ViewAgent ! Stop()
			}
			
			case Start() => {
				startTicketOffice()
				ViewAgent ! Write("START procedure initialized")
				startNodes()
				controllerLoop()
			}
		}
	}

	def act() {
		react{
			case InitController(address) => {
				// Set the Address from witch the publisher will be ready 
				publisherAgent.addListener(address)

				// Pub-Sub service definition
				publisherAgent.registerValuePublisher("events",publisherValue)
				
				controllerLoop()
			}
		}
	}
	
}

case class Write(m:String)
case class DisableButtons()
/**
 * Object used to wrap the View.
 **/
object ViewAgent extends Actor {
	
	val view = new CentralControllerView
	
	def viewAgentLoop() : Unit = react {
		case Write(m:String) => {
			view.write(m)
			viewAgentLoop()
		}
		case DisableButtons() => {
			println("Buttons disabled")
			view.disableButtons()
			viewAgentLoop()
		}
		case Stop() => 
	}
	
	def act() {
		view.setStopOperation(_ => Controller ! DistributedStop())
		view.setStartOperation( _=> Controller ! Start())
		viewAgentLoop()	
	}
}

/**
 *
 * Defines a Message Receiver agent
 *
 **/
class Receiver extends IncomingMessageCallback {
	
	var serverAgent = new Agent
	
	def addHandler (address : String) {
		val add = serverAgent.addListener(address);
		println("Central Controller listening on address " + address)
		serverAgent.registerObject("central_controller",this)
	}
	
	def call(im : IncomingMessage) {
		im.getMessageName match {
			case "event" => {
				Controller ! Event(im.getParameters.getString("event"))
			}
			
			case "central_office_ack" => {
				println("RECEIVED ACK FROM TICKET OFFICE")
				
				im.reply(new Parameters)
				
				// Now that we received the ack, we can proceed with the termiation of the nodes				
				Controller ! Continue()
				
			}
			
			case "node_terminated" => {
				
				Controller ! NodeTerminated(im.getParameters.getString("node_name"))
				
				val replyPar = new Parameters
				replyPar.setString("response","OK")
				im.reply(replyPar)
			}
			case other => {
				print("ERROR : Invalid EVENT "+ other.toString )
			}
		}
	}
	
	def close {
		serverAgent.close
	}
}

object ControllerMain extends App {
	
	var NAME_SERVER_ADDRESS = ""
	var CENTRAL_TICKET_OFFICE_ADDRESS = ""
	
	var receiver 	: Receiver = null
	
	def waitExit {
		readLine() match {
			case "q" | "Q" => {
				Controller ! Stop()
				receiver.close
				println ("Bye!")
			}
		}
	}
	
	override def main(args : Array[String]) {
		
		if (args.length < 3) {
			println ("""
ERROR: Following parameters MUST be specifed:
	1) Controller address;
	2) Name Server address;
	3) Central Ticket Office address""");
			return;
		}
		
		val controllerAddress = args(0)
		val nameServerAddress = args(1)
		val centralTicketOfficeAddress = args(2)
		
		// Controller creation,
		Controller.start
		Controller ! InitController("tcp://localhost:2222")
		
		// Receiver creation
		receiver = new Receiver
		receiver.addHandler(controllerAddress)
		
		// Initialization of the View Agent
		ViewAgent.start
		
		NAME_SERVER_ADDRESS = nameServerAddress
		CENTRAL_TICKET_OFFICE_ADDRESS = centralTicketOfficeAddress
	
		
		waitExit
		
	} 
}
