function Move-Window {
    param (
        [Parameter(Mandatory=$true)]
        [System.Diagnostics.Process]$Process,
        [Parameter(Mandatory=$true)]
        [int]$X,
        [Parameter(Mandatory=$true)]
        [int]$Y
    )

    $hwnd = $Process.MainWindowHandle
    $null = [user32]::MoveWindow($hwnd, $X, $Y, 820, 610, $true)
}

Add-Type @"
using System;
using System.Runtime.InteropServices;

public class user32 {
    [DllImport("user32.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    public static extern bool MoveWindow(IntPtr hWnd, int X, int Y, int nWidth, int nHeight, bool bRepaint);
}
"@

$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Definition

# Rutas de los archivos .jar en la misma carpeta que el script
$jar1 = (Join-Path $scriptPath 'GeneradorExpedienteCSV-1.0.0-all.jar')
$jar2 = (Join-Path $scriptPath 'GeneradorExpedienteJSON-1.0.0-all.jar')
$jar3 = (Join-Path $scriptPath 'NotasMiddleware-1.0.0-all.jar')
$jar4 = (Join-Path $scriptPath 'Visualizer-1.0.0-all.jar')

Write-Host "Path: $scriptPath"

# Mostrar texto en 3D ASCII isométrico
Write-Host @"

 /######  /######  /#######         /######                                                  /##
|_  ##_/ /##__  ##| ##__  ##       /##__  ##                                               /####
  | ##  | ##  \ ##| ##  \ ##      | ##  \__/  /######  /##   /##  /######   /######       |_  ##
  | ##  | ########| #######/      | ## /#### /##__  ##| ##  | ## /##__  ## /##__  ##        | ##
  | ##  | ##__  ##| ##____/       | ##|_  ##| ##  \__/| ##  | ##| ##  \ ##| ##  \ ##        | ##
  | ##  | ##  | ##| ##            | ##  \ ##| ##      | ##  | ##| ##  | ##| ##  | ##        | ##
 /######| ##  | ##| ##            |  ######/| ##      |  ######/| #######/|  ######/       /######
|______/|__/  |__/|__/             \______/ |__/       \______/ | ##____/  \______/       |______/
                                                                | ##
                                                                | ##
                                                                |__/

"@

# Abrir las terminales y ejecutar los archivos .jar
$proc1 = Start-Process powershell -ArgumentList ("-NoExit","-Command","`$Host.UI.RawUI.WindowTitle = 'GeneradorExpedienteCSV'; java -jar '$jar1'") -PassThru
$proc2 = Start-Process powershell -ArgumentList ("-NoExit","-Command","`$Host.UI.RawUI.WindowTitle = 'GeneradorExpedienteJSON'; java -jar '$jar2'") -PassThru
$proc3 = Start-Process powershell -ArgumentList ("-NoExit","-Command","`$Host.UI.RawUI.WindowTitle = 'NotasMiddleware'; java -jar '$jar3'") -PassThru
$proc4 = Start-Process powershell -ArgumentList ("-NoExit","-Command","`$Host.UI.RawUI.WindowTitle = 'Visualizer'; java -jar '$jar4'") -PassThru

# Mover las ventanas a diferentes posiciones
Start-Sleep -Seconds 1  # Esperar un segundo para que las ventanas se abran
Move-Window -Process $proc1 -X 0 -Y 0
Move-Window -Process $proc2 -X 800 -Y 0
Move-Window -Process $proc3 -X 0 -Y 600
Move-Window -Process $proc4 -X 800 -Y 600

# Mensaje para cerrar las terminales
Write-Host "Escribe 'quit' para cerrar todas las terminales."

# Bucle para esperar el comando "quit"
while ($true) {
    $input = Read-Host ">"
    if ($input -eq "quit" -or $input -eq "q") {
        # Cerrar las terminales
        $proc1.CloseMainWindow()
        $proc2.CloseMainWindow()
        $proc3.CloseMainWindow()
        $proc4.CloseMainWindow()
        break
    }
}