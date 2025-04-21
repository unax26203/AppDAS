<?php
$DB_SERVER = "localhost";
$DB_USER = "Xuzardoya001";  
$DB_PASS = "*";
$DB_DATABASE = "Xuzardoya001_appseguimiento";

$con = mysqli_connect($DB_SERVER, $DB_USER, $DB_PASS, $DB_DATABASE);

if (!$con) {
    die("Error de conexion: " . mysqli_connect_error());
}
?>
