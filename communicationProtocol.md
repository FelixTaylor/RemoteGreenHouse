<table>
	<colgroup>
		<col width="50">
		<col width="300">
		<col width="400">
	</colgroup>
	<tr align="center">
		<th> </th>
		<th> controlValues[] </th>
		<th> Bedeutung </th>
	</tr>
	<tr align="center">
		<td> [0] </td>
		<td> LED_Level </td>
		<td> Helligkeit der LED Stripes </td>
	</tr>
	<tr align="center">
		<td> [1] </td>
		<td> LED_turnOnTime </td>
		<td> Anschaltzeit des LED Stripes </td>
	</tr>
	<tr align="center">
		<td> [2] </td>
		<td> LED_turnOffTime </td>
		<td> Ausschaltzeit des LED Stripes </td>
	</tr>
	<tr align="center">
		<td> [3] </td>
		<td> LightSensor_minimumBrightnessLevel </td>
		<td> Grenzwert für Umgebungshelligkeit zum Zuschalten des LED Stripes </td>
	</tr>
	<tr align="center">
		<td> [4] </td>
		<td> LightControlMode </td>
                <td>
                        <table>
                                <tr align="center"> <th> Wert </th>  <th> Bedeutung</th> </tr>
                                <tr align="center"> <td> 0 </td>  <td> Zeitschaltuhr + Lichtsensor</td> </tr>
                                <tr align="center"> <td> 1 </td>  <td> Lichtsensor </td> </tr>
                                <tr align="center"> <td> 2 </td>  <td> Zeitschaltuhr </td> </tr>
                                <tr align="center"> <td> 3 </td>  <td> Dauerhaft an </td> </tr>
                        </table>
                </td>
	</tr>
</table>

<br>
<br>

<table>
	<colgroup>
		<col width="50">
		<col width="100">
		<col width="200">
	</colgroup>
	<tr align="center">
		<th> </th>
		<th> sensorValues[] </th>
		<th> Bedeutung </th>
	</tr>
	<tr align="center">
		<td> [0] </td>
		<td> temperature </td>
		<td> Lufttemperatur [°C] </td>
	</tr>
	<tr align="center">
		<td> [1] </td>
		<td> humidity </td>
		<td> Luftfeuchtigkeit [g/kg] </td>
	</tr>
	<tr align="center">
		<td> [2] </td>
		<td> pressure </td>
		<td> Druck [Pa] </td>
	</tr>
	<tr align="center">
		<td> [3] </td>
		<td> moisture </td>
		<td> Bodenfeuchtigkeit [g/kg] </td>
	</tr>
	<tr align="center">
		<td> [4] </td>
		<td> brightness </td>
		<td> Aussenhelligkeit [lux] </td>
	</tr>
</table>

<br>
<br>

<table>
	<colgroup>
		<col width="100">
		<col width="100">
		<col width="400">
	</colgroup>
	<tr align="center">
		<th> Anfragecode </th>
		<th> ASCII-Nr. </th>
		<th> Bedeutung </th>
	</tr>
	<tr align="center">
		<td> w </td>
		<td> 119 </td>
		<td> Arduino soll Sensorwerte an Android device schicken </td>
	</tr>
	<tr align="center">
		<td> x </td>
		<td> 120 </td>
		<td> Arduino soll die übermittelten ControlValues setzen </td>
	</tr>
	<tr align="center">
		<td> y </td>
		<td> 121 </td>
		<td> Arduino soll die gespeicherten ControlValues übermittelten </td>
	</tr>
</table>

<br>
<br>

<body> Beispiel Android device Anfrage an Arduino: Sendestringstellen </body>
<br>

<table>
	<colgroup>
		<col width="120">
		<col width="120">
		<col width="120">
		<col width="120">
	</colgroup>

	<tr align="center">
		<th> Stringstelle </th>
		<th>  </th>
		<th>  </th>
	</tr>
	<tr align="center">
		<td> 0 </td>
		<td> w </td>
		<td> x </td>
	</tr>
	<tr align="center">
		<td> 1 </td>
		<td> dayTime [ms] </td>
		<td> dayTime [ms] </td>
	</tr>
	<tr align="center">
		<td> 2 </td>
		<td> ; </td>
		<td> ; </td>
	</tr>
	<tr align="center">
		<td> 3 </td>
		<td>  </td>
		<td> controlValues[0] </td>
	</tr>
	<tr align="center">
		<td> 4 </td>
		<td>  </td>
		<td> ; </td>
	</tr>
	<tr align="center">
		<td> 5 </td>
		<td>  </td>
		<td> controlValues[1] </td>
	</tr>
	<tr align="center">
		<td> 6 </td>
		<td>  </td>
		<td> ; </td>
	</tr>
	<tr align="center">
		<td> 7 </td>
		<td>  </td>
		<td> controlValues[2] </td>
	</tr>
	<tr align="center">
		<td> 8 </td>
		<td>  </td>
		<td> ; </td>
	</tr>
	<tr align="center">
		<td> 9 </td>
		<td>  </td>
		<td> controlValues[3] </td>
	</tr>
	<tr align="center">
		<td> 10 </td>
		<td>  </td>
		<td> ; </td>
	</tr>
	<tr align="center">
		<td> 11 </td>
		<td>  </td>
		<td> controlValues[4] </td>
	</tr>
	<tr align="center">
		<td> 12 </td>
		<td>  </td>
		<td> ; </td>
	</tr>
</table>

<br>
<br>

<body> Beispiel Arduino antwortet auf Android device Anfrage: Sendestringstellen </body>
<br>

<table>  
	<colgroup>
		<col width="120">
		<col width="120">
		<col width="120">
		<col width="120">
	</colgroup>

	<tr align="center">
		<th> Stringstelle </th>
		<th> w </th>
		<th> x </th>
		<th> y </th>
	</tr>
	<tr align="center">
		<td> 0 </td>
		<td> sensorValues[0] </td>
		<td>  - </td>
		<td>  s </td>
	</tr>
	<tr align="center">
		<td> 1 </td>
		<td> ; </td>
		<td>  - </td>
		<td> controlValues[0] </td>
	</tr>
	<tr align="center">
		<td> 2 </td>
		<td> sensorValues[1] </td>
		<td>  - </td>
		<td> ; </td>
	</tr>
	<tr align="center">
		<td> 3 </td>
		<td> ; </td>
		<td>  - </td>
		<td> controlValues[1] </td>
	</tr>
	<tr align="center">
		<td> 4 </td>
		<td> sensorValues[2] </td>
		<td>  - </td>
		<td> ; </td>
	</tr>
	<tr align="center">
		<td> 5 </td>
		<td> ; </td>
		<td>  - </td>
		<td> controlValues[2] </td>
	</tr>
	<tr align="center">
		<td> 6 </td>
		<td> sensorValues[3] </td>
		<td>  - </td>
		<td> ; </td>
	</tr>
	<tr align="center">
		<td> 7 </td>
		<td> ; </td>
		<td>  - </td>
		<td> controlValues[3] </td>
	</tr>
	<tr align="center">
		<td> 8 </td>
		<td> sensorValues[4] </td>
		<td>  - </td>
		<td> ; </td>
	</tr>
	<tr align="center">
		<td> 9 </td>
		<td> ; </td>
		<td>  - </td>
		<td> controlValues[4] </td>
	</tr>
	<tr align="center">
		<td> 10 </td>
		<td>  </td>
		<td>  - </td>
		<td> ; </td>
	</tr>
</table>