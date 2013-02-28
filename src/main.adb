--==============================================================================
-- File:
--	main.adb
-- Author:
--	Moreno Ambrosin
--	Mat.  : 1035635
-- Date:
-- 	09/02/2013
--==============================================================================

with YAMI.Outgoing_Messages; use YAMI.Outgoing_Messages;
with YAMI.Agents;
with YAMI.Parameters;

with Ada.Command_Line;
with Ada.Exceptions;
with Ada.Text_IO;


with Environment;use Environment;
with Trains;
with Route;use Route;
with Train_Pool;
With Task_Pool;

with Logger;

with Traveler;

procedure Main is
begin

	if Ada.Command_Line.Argument_Count /= 1 then
		Ada.Text_IO.Put_Line("Expecting a log level [ -i | -n | -d ].");
		Ada.Command_Line.Set_Exit_Status(Ada.Command_Line.Failure);
		return;
	end if;

	if(not Logger.Init(Ada.Command_Line.Argument (1))) then
		Ada.Text_IO.Put_Line("Wrong parameter! Use a valid log level [ -i | -n | -d ].");
		return;
	end if;

	declare
		-- Creation of Actors for Travelers
		Traveler_Tasks 	: Task_Pool.Task_Pool_Type(5);
		Pool 			: Train_Pool.Train_Task_Pool(3);
	begin

--  		Train_Pool.Associate(Trains.Trains(1));
--  		Train_Pool.Associate(Trains.Trains(2));
			for J in 1 .. Environment.Travelers'Length loop
				Traveler.Print(Environment.Travelers(J));
			end loop;
	end;


--	declare
--	  	Server_Address : constant String := Ada.Command_Line.Argument (1);
--	  	Client_Agent : YAMI.Agents.Agent := YAMI.Agents.Make_Agent;
--	begin
--		--  read lines of text from standard input
--		--  and post each one for transmission
--    	while not Ada.Text_IO.End_Of_File loop
--       		declare
--				Input_Line : constant String := Ada.Text_IO.Get_Line;
--				Params : YAMI.Parameters.Parameters_Collection := YAMI.Parameters.Make_Parameters;
--			begin
--		        --  the "content" field name is arbitrary,
--		        --  but needs to be recognized at the server side
--		        Params.Set_String ("content", Input_Line);
--		        Params.Set_String ("prova", "Sciao belo");
--		        Client_Agent.Send_One_Way (Server_Address, "printer", "print", Params);
--		    end;
--      	end loop;
--	end;

exception
	when E : others => Ada.Text_IO.Put_Line(Ada.Exceptions.Exception_Message (E));
end Main;
