<?php
header('Content-Type: application/json');
include 'conexion.php';

$sql = "SELECT COUNT(*) AS pending_count FROM media_items WHERE is_completed = 0";
$result = $con->query($sql);

if ($result) {
    $row = $result->fetch_assoc();
    echo json_encode(["pending_count" => $row['pending_count']]);
} else {
    echo json_encode(["error" => $con->error]);
}

$con->close();
?>