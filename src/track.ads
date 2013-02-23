with Train;use Train;

package Track is
	
	-- Mantains unique ID of currently travelling trains
	type Train_Queue is array (Positive range <>) of Integer;
	
	protected type Track_Type is
		
		entry Leave(T : Train_Descriptor);
		entry Enter(To_Add :  in out Train_Descriptor; Max_Speed : out Integer);
	
	private 
	
		Free : Boolean := True;
		
		Retry_Num : Integer := 0;
		
		Track_Max_Speed : Integer := 200; 
	
	end Track_Type;
	

end Track;