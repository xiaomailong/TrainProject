package body Plattform is

	protected body Plattform_Type is
		entry Enter(Descriptor : in out Train_Descriptor) when Free = True is
		begin 
			Free := False;
		end Enter;
		
		procedure Leave(Descriptor : in out Train_Descriptor) is
		begin 
			Free := True;
		end Leave;
	end Plattform_Type;
	
end Plattform;