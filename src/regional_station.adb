with Ada.Text_IO;use Ada.Text_IO;

package body Regional_Station is

	-- Definition of Inherited Methods --
	procedure Enter(
		This : Regional_Station_Type;
		Descriptor : in out Train.Train_Descriptor;
		Plattform : Integer) is
	begin
		This.Plattforms(Plattform).Enter(Descriptor);
	end Enter;

	procedure Leave(
		This : Regional_Station_Type;
		Descriptor : in out Train.Train_Descriptor;
		Plattform : Integer) is
	begin
		This.Plattforms(Plattform).Leave(Descriptor);
	end Leave;

	--
	-- Procedure called by a Traveler to enqueue at a given Platform
	-- waiting for a specific Train
	--
	--
	procedure WaitForTrain(
			This 	: Regional_Station_Type;
			Incoming_Traveler 	: in out Traveler.Traveler_Manager;
			Plattform 	: Integer) is
	begin
		This.Plattforms(Plattform).AddOutgoingTraveler(Incoming_Traveler);
		This.Panel.SetStatus(
			"User " & Traveler.GetName(Incoming_Traveler) & " entered plattform " & Integer'Image(Plattform));
	end WaitForTrain;

	--
	-- Creates a new Station instance
	--
	-- @return: A reference of the new created Station
	--
	function NewRegionalStation(
		Plattforms_Number : Positive;
		Name : Positive) return Station_Ref
	is
		Station : access Regional_Station_Type := new Regional_Station_Type(Plattforms_Number);
	begin
		Station.Name := Name;
		for I in Positive range 1..Plattforms_Number loop
			Station.Plattforms(I) := new Plattform.Plattform_Type(I);
		end loop;
		Station.Panel := new Notice_Panel.Notice_Panel_Entity(Name);
		return Station;
	end;

end Regional_Station;
