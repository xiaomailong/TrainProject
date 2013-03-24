----------------------------------------------------------------------------------
--  Copyright 2013                                								--
--  Moreno Ambrosin                         									--
--  Railway_Simulation 1.0                                       				--
--  Concurrent and Distributed Systems class project  							--
--  Master Degree in Computer Science                 							--
--  Academic year 2012/2013                              						--
--  Dept. of Pure and Applied Mathematics             							--
--  University of Padua, Italy                        							--
--                                                    							--
--  This file is part of Railway_Simulation project.							--
--																				--
--  Railway_Simulation is free software: you can redistribute it and/or modify	--
--  it under the terms of the GNU General Public License as published by		--
--  the Free Software Foundation, either version 3 of the License, or			--
--  (at your option) any later version.											--
--																				--
--  Railway_Simulation is distributed in the hope that it will be useful,		--
--  but WITHOUT ANY WARRANTY; without even the implied warranty of				--
--  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the				--
--  GNU General Public License for more details.								--
--																				--
--  You should have received a copy of the GNU General Public License			--
--  along with Railway_Simulation.  If not, see <http://www.gnu.org/licenses/>. --
----------------------------------------------------------------------------------
with Logger;
with Ada.Containers;use Ada.Containers;
with Ada.Text_IO;use Ada.Text_IO;

package body Task_Pool is

	--
	-- Task type Executor definition
	--
	task body Actor is

		NAME : constant String := "Task_Pool.Actor";

		To_Execute : Any_Operation;

	begin
		loop
			Logger.Log(
				NAME,
				"Task waits for an operation to Execute " & Count_Type'Image(Operations_Queue.Current_Use),
				Logger.DEBUG);

			Operations_Queue.Dequeue(To_Execute);

			Logger.Log(NAME,"Task retrieved an Operation",Logger.DEBUG);

			-- Right Here, I'm shure to have an Operation to Execute
			To_Execute.Do_Operation;
		end loop;
    end Actor;

	--
	-- Adds the given Operation Pointer to the Operations queue
	--
	procedure Execute(Operation : Any_Operation) is
	begin
		Operations_Queue.Enqueue(Operation);
		Logger.Log(
				"Task_Pool",
				"Operation Added; total number : " & Count_Type'Image(Operations_Queue.Current_Use),
				Logger.DEBUG);
	end Execute;

end Task_Pool;