<?php
/*
 * Copyright 2010-2016 Frans-Willem Hardijzer
 *
 * This file is part of BitonSync.
 *
 * BitonSync is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BitonSync is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BitonSync.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
Door: Frans-Willem Hardijzer
Wanneer: 09-12-2011 13:30
Voor: Synchroniseren ledenlijst met Android telefoons
Waarom: Super handig.
Veilig?: Ja, hij checked je password netjes en geeft geen ledenlijst zonder kloppende username/wachtwoord combo.
*/
header("Content-Type: text/plain");
ob_start(); //Intercept output from wp-load
require_once( dirname(__FILE__) . '/wp-load.php' );
ob_end_clean();
$credentials=array(
	'user_login' => $_POST['log'],
	'user_password' => $_POST['pwd'],
	'remember' => false
);
function startswith($str,$haystack) {
	return (substr($str,0,strlen($haystack)) == $haystack);
}
function page_to_json($page) {
	$data=$page->post_content;
	if (!preg_match_all("|<th( [^>]*)?".">(.*?)</th>|is",$data,$thmatches) || !preg_match_all("|<td( [^>]*)?".">(.*?)</td>|is",$data,$tdmatches))
		return "";
	$headers=$thmatches[2];
	$columns=$tdmatches[2];
	$people=array();
	for ($i=0; $i<count($columns)/count($headers); $i++) {
		$people[$i]=array();
		for ($j=0; $j<count($headers); $j++) {
			$people[$i][strtolower($headers[$j])]=strip_tags($columns[($i*count($headers))+$j]);
		}
	}
	for ($i=0; $i<count($people); $i++) {
		if (preg_match("|(^.+) \\((.+?)\\) (.+$)|",$people[$i]['naam'],$naammatch)) {
			$people[$i]['naam']=$naammatch[1];
			$people[$i]['voornaam']=$naammatch[2];
			$people[$i]['achternaam']=$naammatch[3];
		} else {
			$naammatch=explode(" ",$people[$i]['naam'],2);
			$people[$i]['naam']=$people[$i]['voornaam']=$naammatch[0];
			$people[$i]['achternaam']=$naammatch[1];
		}
		if (preg_match("|^(.*?) ([0-9]{4} [A-Z]{2}) (.*?)$|",$people[$i]['adres'],$adresmatch)) {
			$people[$i]['adres']=$adresmatch[1].", ".$adresmatch[2].", ".$adresmatch[3];
		}
		if (preg_match("|([0-9]+) ([A-Za-z]+) ([0-9]+)|",$people[$i]['geboortedatum'],$geboortematch)) {
			$day=$geboortematch[1];
			$month=strtolower($geboortematch[2]);
			$year=$geboortematch[3];
			if (startswith($month,"ja")) $month=1;
			else if (startswith($month,"f")) $month=2;
			else if (startswith($month,"ma")) $month=3;
			else if (startswith($month,"ap")) $month=4;
			else if (startswith($month,"me")) $month=5;
			else if (startswith($month,"jun")) $month=6;
			else if (startswith($month,"jul")) $month=7;
			else if (startswith($month,"au")) $month=8;
			else if (startswith($month,"s")) $month=9;
			else if (startswith($month,"o")) $month=10;
			else if (startswith($month,"n")) $month=11;
			else if (startswith($month,"d")) $month=12;
			$people[$i]['geboortedatum']=$year."-".$month."-".$day;
		}
		$people[$i]['mobiel']=preg_replace("|[^0-9]|","",$people[$i]['mobiel']);
		$people[$i]['vaste telefoon']=preg_replace("|[^0-9]|","",$people[$i]['vaste telefoon']);
		if ($people[$i]['mobiel'][0] == '0')
			$people[$i]['mobiel']="+31".substr($people[$i]['mobiel'],1);
		if ($people[$i]['vaste telefoon'][0] == '0')
			$people[$i]['vaste telefoon']="+31".substr($people[$i]['vaste telefoon'],1);
	}
	return json_encode($people);
}
$user=wp_signon($credentials,false);
if (is_wp_error($user)) {
	echo "ERR ".strip_tags($user->get_error_message());
} else {
	wp_set_current_user($user->ID);
	switch ($_POST['m']) {
		case 'auth':
			echo "OK Login accepted";
			break;
		case 'data':
			echo "OK ".page_to_json(get_page_by_path("ledenlijst"));
			break;
		default:
			echo "ERR Unknown method";
			break;
	}
}
?>
