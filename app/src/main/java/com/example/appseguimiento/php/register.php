<?php
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);
include("conexion.php");

$nombre = $_POST['nombre'];
$password = $_POST['password'];
$hash = password_hash($password, PASSWORD_DEFAULT);


$sql = "INSERT INTO usuarios (nombre, password) VALUES (?, ?)";
$stmt = mysqli_prepare($con, $sql);
mysqli_stmt_bind_param($stmt, "ss", $nombre, $hash);
$result = mysqli_stmt_execute($stmt);

echo $result ? "OK" : "ERROR";
?>
