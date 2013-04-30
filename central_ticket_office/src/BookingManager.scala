import scala.actors._
import net.minidev.json._

/**
 * This object is responsible of serializing the validation requests.
 */
object BookingManager extends Actor {
	
	/**
	 * All FB Trains 
	 */
	var trainRouteMap : Map[Int,Train] = Route.loadTrainsRoutesMap("../../railway/res/trains.json")
	
	/**
	 * All the routes
	 */
	var routes : Array[Route] = Route.loadRoutes("../../railway/res/routes.json")
	
	import java.text.SimpleDateFormat
	import java.util.Date
	import java.util.TimeZone

	
	/**
	 * This class represents a Timetable for a Route. It specifies the index of the route, the number of runs 
	 * covered by the time table and the span used to update the time table.
	 */ 
	class RouteTimeTable(val routeIndex:Int,val runs:Int,val span:Int) {
		
		/*
		 * The run index.
		 */
		var current_run : Int = 0
		
		/**
		 * The timeTable
		 */
		var table : Array[Array[Date]] = new Array(runs)
		
		/*
		 * This Array contains the list of spans, useful to populate and update the timeTable
		 */
		var spans_table : Array[Array[Int]] = new Array(runs)
		
		/**
		 * Simple debug method to print the Time Table.
		 */
		def printDate {
			println("Time Table for route : " + routeIndex )
			println("Runs number : " + runs )
			println("Current Run : " + current_run )
			table.foreach(list => {
				list.foreach(date => {
					// Print on a specific format
					val format = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss")
					// Set timezone to GMT, to print UTC-0 time 
					format.setTimeZone(TimeZone.getTimeZone("GMT"))
					print(new StringBuilder( format.format(date) + " , "))
				})
				println
			})
		}
		
		/**
		 * Returns a JSON representation of the RouteTimeTable object.
		 */
		def toJSON : String = {
			var timeTable = "{" 
			timeTable += "\"route_index\" : " + (routeIndex+1) + ","
			//timeTable += "\"restart_span\" : " + span + ","
			timeTable += "\"current_run\" : " + (current_run+1) + ","
			timeTable += "\"time\" : ["
			var i : Int = 1
			for (i<- 0 until table.size) {
				timeTable += "["
				var j = 0
				table(i).foreach(date => {
					// Print on a specific format
					val format = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss")
					// Set timezone to GMT, to print UTC-0 time 
					format.setTimeZone(TimeZone.getTimeZone("GMT"))
					timeTable += "\"" + new StringBuilder( format.format(date)) + "\""
					if (j < table(i).size - 1) timeTable += ","
					j += 1
				})
				timeTable += "]"
				if (i < table.size - 1) timeTable += ","
			
			}
			
			timeTable += "]}"
			timeTable
		}
		
	}
	
	/**
	 * The time table for each route.
	 */
	private var timeTables : Array[RouteTimeTable] = new Array(routes.size)
	
	// load the time table JSON file.
	val jsonTimeTable = scala.io.Source.fromFile("../../railway/res/time_table.json").mkString
	
	// reference time from which build the time table.
	val ref = new Date 
	
	// Loading time table
	JSONValue.parseStrict(jsonTimeTable) match {
		case o : JSONObject => o.get("time_table") match {
			case timeTable : JSONArray => {
				println(timeTable.size)
				for (j <- 0 until timeTable.size) {
					timeTable.get(j) match {
						case runTable : JSONObject => {
							val routeIndex : Int = runTable.get("route") match {
								case o : java.lang.Integer => o.intValue
							}
							val span : Int = runTable.get("restart_span") match {
								case o : java.lang.Integer => o.intValue
							}
							
							runTable.get("time") match {
								case runs : JSONArray => {
									var r = new RouteTimeTable((routeIndex-1),runs.size,span)
									
									for (k <- 0 until runs.size) {
										runs.get(k) match {
											case run : JSONArray => {
												
												r.spans_table(k) 	= new Array(run.size)
												r.table(k) 			= new Array(run.size)
												
												for (h <- 0 until run.size) {
													run.get(h) match {
														case i : java.lang.Integer => {
															// Add an element to the current Time table
															r.table(k)(h) = new Date(ref.getTime+(i*1000))
															// Save also the span
															r.spans_table(k)(h) = (i*1000)
														}
													}
												}
											}
										}
									}
									timeTables(j) = r
//									timeTables(j).printDate
//									timeTables(j).spans_table.foreach(sp => {sp.foreach(s => print(s + "  "));println})
								}
							}
							
						}
					}
				}
			} 
		}
		
	}
	
	/**
	 * An Array where for each route contains the number of free sits.
	 */
	var bookingSits : Map[Int,Array[Array[Int]]] = Map()

	// Initialization. Create an entry for each Train in trainRouteMap. This is done
	// because only booking info for FB Trains is needed.
	trainRouteMap.keys.foreach(trainId => {
		val t : Train = trainRouteMap(trainId)
		val timeTable = timeTables(t.routeIndex)
		var bookingSitsElem : Array[Array[Int]] = new Array(timeTable.table.size)
		for(i <- 0 until bookingSitsElem.size) {
			bookingSitsElem(i) = Array.fill(timeTable.table(i).size)(t.sitsNumber)
		}
		
		bookingSits = bookingSits + Tuple2(t.routeIndex,bookingSitsElem) 
	})
	
	
	// Debug print  
	def printBookingSits {
		bookingSits.keys.foreach(k => {
			bookingSits(k).foreach(array => {
				array.foreach(el => print(el+ " , "))
				println
			})
		
		})
	}
	
	
	/**
	 * Returns a JSON string representation of the all the time tables
	 *
	 * @return the time table in JSON
	 **/
	def timeTablesToJSON : String = {
		var jsonTimeTables = "{"
		jsonTimeTables += "\"time_table\" : ["
		for (i <- 0 until timeTables.size) {
			jsonTimeTables += timeTables(i).toJSON
			if (i < timeTables.size-1) jsonTimeTables += ","
		}
		jsonTimeTables += "]}"
		jsonTimeTables
	}
	
	
	println(timeTablesToJSON)
	
	/** 
	 * Main loop for the Actor.
	 */
	def bookingLoop() {
		react {
			
			
			case GetTimeTable => {
				// Simply return the JSON representation of the Time Table.
				reply{timeTablesToJSON}
				bookingLoop
			}
			
			
			// Updates the current run index. It is done keeping always at least 2 route time tables,
			// so if the updates reguardes 
			case UpdateRun(routeIndex,current_run) => {
				// Update the RouteTimeTable element in position [routeIndex] with 
				// the given current_run value.
				// Check if the given routeIndex is a valid one
				if ((routeIndex-1) >= timeTables.size) reply(("error","Invalid routeIndex " + routeIndex))
				// Check if the current_run is a valid one
				else if ((current_run-1) >= timeTables(routeIndex-1).table.size) reply(("error","Invalid current_run " + current_run))
				else {		
					// All ok, update the run	
					timeTables(routeIndex-1).current_run = (current_run-1)
					// If the current run is the penultimate, update the entire 
					// table and give it back to the sender.
					if ((current_run-1) == timeTables(routeIndex-1).table.size-2) {
						
						val newTable : Array[Array[Date]]= new Array(timeTables(routeIndex-1).runs)
						// Put last two elements on the first two positions
						newTable(0) = timeTables(routeIndex-1).table(timeTables(routeIndex-1).table.size-2)
						newTable(1) = timeTables(routeIndex-1).table(timeTables(routeIndex-1).table.size-1)
						
						// Copy all other Time Tables, adding new span!
						for(i <- 0 until timeTables(routeIndex-1).runs-2) {
							newTable(i+2) = timeTables(routeIndex-1).table(i)
							for (j<- 0 until newTable(i+2).size) {
								newTable(i+2)(j) = new Date(newTable(1).last.getTime + timeTables(routeIndex-1).spans_table(i)(j)) 
							}
							
						}
						// Finally update the timeTable!
						timeTables(routeIndex-1).table = newTable
						// Set current_run to 0 .
						timeTables(routeIndex-1).current_run = 0
						
						println("New Time Table for route " + (routeIndex-1) + ":\n" + timeTables(routeIndex-1).toJSON)			
												
						// Give it back to Sender
						reply(("new_time_table",timeTables(routeIndex-1).toJSON))
						 
						
					}else{
						println("Current Run for route " + (routeIndex-1) + " updated : " + (current_run-1))	
						reply("updated")
					}
				}
				bookingLoop
			}
			
			// Validates a list of ticket. They have to be for a unique Region, otherwise
			// it would not be able to validate.
			case Validate(ticketList,requestTime) => {
				
				// Get the Date object corresondig to the specified requestTime
				val requestTimeDate = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").parse(requestTime)
				
				println("VALIDATION REQUEST TIME = " + new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(requestTimeDate))
				
				// Map used to keep track of the sits to edit once the sit is booked.
				var bookingSitsToUpdate : List[Map[Int,(Int,Int)]] = List()
				
				var valid : Boolean = true
				
				for (i <- 0 until ticketList.size;if (valid)) {
					
					var ticket = ticketList(i)
					
					var routeMap : Map[Int,(Int,Int)] = Map();
					
					// Run over all the stages
					for (j <- 0 until ticket.stages.size;if (valid)) {
					
						var ticketStage = ticket.stages(j)
						
						trainRouteMap get (ticketStage.trainId) match {
							// Only if the train is a FB Train the ticket must be validated.
							case Some(map) => {
								// Retrieve the route index
								val routeIndex = map.routeIndex
							
								var firstIndex 	= -1
								var secondIndex = -1		
							
								println("Curren index = " + routeIndex)
								println("startStation = " + ticketStage.startStation)
								println("nextStation  = " + ticketStage.nextStation)
					
								// check in first half
								for (i <- 0 until routes(routeIndex).stages.size/2) {
									
									if (routes(routeIndex).stages(i).startStation 	== ticketStage.startStation && 
										routes(routeIndex).stages(i).nodeName 		== ticketStage.nextRegion) {
										firstIndex = i 
									}
									if (routes(routeIndex).stages(i).nextStation 	== ticketStage.nextStation &&
										routes(routeIndex).stages(i).nodeName 		== ticketStage.nextRegion) {
										secondIndex = i
									}
									
								}
								
								if (firstIndex > secondIndex || firstIndex == -1 || secondIndex == -1) {
									// If we haven't a match in the first half, search in the second half.
									for (i <- routes(routeIndex).stages.size/2 until routes(routeIndex).stages.size) {
										
										if (routes(routeIndex).stages(i).startStation	== ticketStage.startStation&& 
											routes(routeIndex).stages(i).nodeName 		== ticketStage.nextRegion) {
											firstIndex = i 
										}
										
										if (routes(routeIndex).stages(i).nextStation 	== ticketStage.nextStation&& 
											routes(routeIndex).stages(i).nodeName 		== ticketStage.nextRegion) {
											secondIndex = i
										}
									}
								}
								
								// decide weather to search in the current run or in the next one
								val selected_run = {
									val current_run = timeTables(routeIndex).current_run
									if (timeTables(routeIndex).table(current_run)(firstIndex).after(requestTimeDate)) 
										timeTables(routeIndex).current_run
									else
										timeTables(routeIndex).current_run + 1
								}
								
								// At this point firstIndex will be the first index of the route,
								// secondIndex the last. 
								// Check if there are enougth sits to assign to the Traveler
								for (i <- firstIndex to secondIndex; if (valid)) {
									valid = valid && bookingSits(routeIndex)(selected_run)(i) > 0
								}
								
								// Add to routeMap, to be updated if the ticket will result validated
								if (valid) {
									routeMap = routeMap + Tuple2(routeIndex,(firstIndex,secondIndex))
									// memorize the run
									ticketStage.run_number = selected_run+1
								}
							}
							case None => println("Train " + ticketStage.trainId + " not a FB train ")
						}
					}
					
					bookingSitsToUpdate = bookingSitsToUpdate :+ routeMap
									
				}
				// If we arrived here we have all sits needed. We can perform the update
				// Finally, send the reply
				if (valid) {
					bookingSitsToUpdate.foreach( el =>
						el.keys.foreach( routeIndex => {
							val firstIndex = el(routeIndex)._1
							val secondIndex = el(routeIndex)._2
							for (i <- firstIndex to secondIndex) {
								bookingSits(routeIndex)(timeTables(routeIndex).current_run)(i) -= 1
							}
							println(routeIndex +","+firstIndex+ ","+secondIndex)
						})
					)
					reply ((true,ticketList))
				}
				if (!valid) reply(false)
				
				printBookingSits
				
				
				bookingLoop
			}
			
			
			case Stop => {
				println("Validator shutted down")
			}
		}
	}
	
	def act = bookingLoop
}

