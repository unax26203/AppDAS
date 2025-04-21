<?php
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    // Obtener datos del formulario
    $tokenDestino = $_POST['token'] ?? '';
    $mensajeTexto = $_POST['mensaje'] ?? 'Mensaje de prueba';
    
    if (empty($tokenDestino)) {
        die("Debes introducir un token de destino.");
    }

    // Cargar credenciales
    $serviceAccount = json_decode(file_get_contents('appdas-34457-62dcf11247d4.json'), true);

    // Crear JWT
    $jwtHeader = base64_encode(json_encode(['alg' => 'RS256', 'typ' => 'JWT']));
    $now = time();
    $jwtClaimSet = base64_encode(json_encode([
        "iss" => $serviceAccount['client_email'],
        "scope" => "https://www.googleapis.com/auth/firebase.messaging",
        "aud" => "https://oauth2.googleapis.com/token",
        "iat" => $now,
        "exp" => $now + 3600,
    ]));
    $jwtToSign = $jwtHeader . '.' . $jwtClaimSet;

    $privateKey = openssl_pkey_get_private($serviceAccount['private_key']);
    openssl_sign($jwtToSign, $signature, $privateKey, "sha256");
    $jwtSigned = $jwtToSign . '.' . base64_encode($signature);

    // Solicitar token de acceso
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, "https://oauth2.googleapis.com/token");
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query([
        'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
        'assertion' => $jwtSigned
    ]));
    $response = curl_exec($ch);
    $responseData = json_decode($response, true);
    curl_close($ch);

    if (!isset($responseData['access_token'])) {
        die("Error al obtener token de acceso: " . $response);
    }
    $accessToken = $responseData['access_token'];
    $projectId = $serviceAccount['project_id'];

    // Preparar mensaje
    $mensaje = [
        "message" => [
            "token" => $tokenDestino,
            "notification" => [
                "title" => "¡Mensaje desde el servidor!",
                "body" => $mensajeTexto
            ],
            "data" => [
                "mensaje" => $mensajeTexto,
                "fecha" => date("d/m/Y H:i:s")
            ]
        ]
    ];

    // Enviar mensaje a FCM
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, "https://fcm.googleapis.com/v1/projects/$projectId/messages:send");
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        "Authorization: Bearer $accessToken",
        "Content-Type: application/json"
    ]);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($mensaje));
    $result = curl_exec($ch);
    curl_close($ch);

    echo "<h3>Respuesta de FCM:</h3><pre>$result</pre>";
}
?>

<!-- HTML del formulario -->
<h2>Enviar mensaje FCM</h2>
<form method="POST">
    <label>Token del dispositivo:</label><br>
    <textarea name="token" rows="4" cols="80" required></textarea><br><br>

    <label>Mensaje a enviar:</label><br>
    <input type="text" name="mensaje" size="60" value="Este es un mensaje de prueba"><br><br>

    <input type="submit" value="Enviar mensaje">
</form>