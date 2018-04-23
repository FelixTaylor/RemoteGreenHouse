<table>
	<tr>
		<th> </th>
		<th> Control Values </th>
		<th> Bedeutung </th>
	</tr>
	<tr>
		<td> [0] </td>
		<td> LED_Level </td>
		<td> Helligkeit der LED Stripes </td>
	</tr>
	<tr>
		<td> [1] </td>
		<td> LED_turnOnTime </td>
		<td> Anschaltzeit des LED Stripes </td>
	</tr>
	<tr>
		<td> [2] </td>
		<td> LED_turnOffTime </td>
		<td> Ausschaltzeit des LED Stripes </td>
	</tr>
	<tr>
		<td> [3] </td>
		<td> LightSensor_minimumBrightnessLevel </td>
		<td> Grenzwert für Umgebungshelligkeit zum Zuschalten des LED Stripes </td>
	</tr>
	<tr>
		<td> [4] </td>
		<td> LightControlMode </td>
		<td> Ausgewählter Lichtmodus: 0-Zeitschaltuhr und Lichtsensor; 1-Lichtsensor; 2-Zeitschaltuhr; 3-Dauerhaft an</td>
	</tr>
</table>

<table>
	<tr>
		<th> </th>
		<th> Sensor Values </th>
		<th> Bedeutung </th>
	</tr>
	<tr>
		<td> [0] </td>
		<td> temperature </td>
		<td> Lufttemperatur [°C] </td>
	</tr>
	<tr>
		<td> [1] </td>
		<td> humidity </td>
		<td> Luftfeuchtigkeit [g/kg] </td>
	</tr>
	<tr>
		<td> [2] </td>
		<td> pressure </td>
		<td> Druck [Pa] </td>
	</tr>
	<tr>
		<td> [3] </td>
		<td> moisture </td>
		<td> Bodenfeuchtigkeit [g/kg] </td>
	</tr>
	<tr>
		<td> [4] </td>
		<td> brightness </td>
		<td> Aussenhelligkeit [lux] </td>
	</tr>
</table>

<table>
	<tr>
		<th> Anfragecode </th>
		<th> ASCII-Nr. </th>
		<th> Bedeutung </th>
	</tr>
	<tr>
		<td> w </td>
		<td> 119 </td>
		<td> Arduino soll Sensorwerte an Android device schicken </td>
	</tr>
	<tr>
		<td> x </td>
		<td> 120 </td>
		<td> Arduino soll die übermittelten ControlValues setzen </td>
	</tr>
</table>

<body> 
	<h1> Beispiel: Sendestringstellen getrennt durch limiterbyte ";" </h1>
</body>

<table>
	<tr>
		<th> Stringstelle </th>
		<th> 0 </th>
		<th> 1 </th>
		<th> 2 </th>
		<th> 3 </th>
		<th> 4 </th>
		<th> 5 </th>
		<th> 6 </th>
	</tr>
	<tr>
		<td> w </td>
		<td> dayTime [ms] </td>
		<td> sensorValues[0] </td>
		<td> sensorValues[1] </td>
		<td> sensorValues[2] </td>
		<td> sensorValues[3] </td>
		<td> sensorValues[4] </td>
	</tr>
	<tr>
		<td> x </td>
		<td> dayTime [ms] </td>
		<td> </td>
		<td> </td>
		<td> </td>
		<td> </td>
		<td> </td>
	</tr>
</table>