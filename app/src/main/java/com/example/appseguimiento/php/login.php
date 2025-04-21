<?php
include("conexion.php");

$nombre = $_POST['nombre'];
$password = $_POST['password'];

$sql = "SELECT * FROM usuarios WHERE nombre = ?";
$stmt = mysqli_prepare($con, $sql);
mysqli_stmt_bind_param($stmt, "s", $nombre);
mysqli_stmt_execute($stmt);
$resultado = mysqli_stmt_get_result($stmt);

if ($fila = mysqli_fetch_assoc($resultado)) {
    if (password_verify($password, $fila['password'])) {
        echo json_encode(array("status" => "OK", "nombre" => $fila["nombre"], "id" => $fila["id"]));
    } else {
        echo json_encode(array("status" => "ERROR", "msg" => "Contrasena incorrecta"));
    }
} else {
    echo json_encode(array("status" => "ERROR", "msg" => "Usuario no encontrado"));
}
?>
