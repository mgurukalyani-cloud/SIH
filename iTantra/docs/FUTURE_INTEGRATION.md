# iTantra — Future Hardware Integration Roadmap

The modular `CommunicationTransport` interface is engineered to interface seamlessly with embedded communication hardware beyond phone-to-phone Bluetooth and Wi-Fi Direct.

---

## 📡 1. LoRa Physical Transceiver Integration (Semtech SX1262 / EBYTE E22)

### Channel Specs:
- **Frequency**: 865 – 867 MHz (License-free Indian ISM band under WPC regulations).
- **Modulation**: LoRa Spread Spectrum, Spreading Factor SF9 to SF11, Bandwidth 125 kHz.
- **Link Budget**: +22 dBm TX power, -137 dBm sensitivity $\rightarrow$ **12 – 15 km Line-of-Sight range**.

```
[Smartphone iTantra App]
         │  (USB-OTG / Bluetooth BLE Bridge)
         ▼
[ESP32 / nRF52840 + SX1262 LoRa Radio Module]
         │  (866 MHz Sub-GHz RF Burst)
         ▼
[Remote SX1262 LoRa Relay Node]
         │
         ▼
[Receiver Smartphone iTantra App]
```

### Transport Adapter:
```kotlin
class LoRaSerialTransport(private val usbSerialPort: UsbSerialPort) : CommunicationTransport {
    override suspend fun send(packet: iMFPPacket): Result<Unit> {
        // Broadcast raw 42-byte iMFP frame directly via SX1262 AT command
        usbSerialPort.write(packet.toByteArray(), 1000)
        return Result.success(Unit)
    }
}
```

---

## 📻 2. Tactical VHF/UHF & APRS Radio Integration

- **Modulation**: Bell 202 Audio Frequency Shift Keying (AFSK) at 1200 baud (1.2 kbps) over existing handheld radios (Baofeng, Kenwood, Motorola).
- **Physical Interface**: 3.5mm TRRS audio cable connecting phone headset jack to radio MIC/SPK with VOX or PTT GPIO.
- **Protocol**: iMFP frames encapsulate directly into standard AX.25 UI frames.

---

## 🛰️ 3. Satellite Communication (NavIC / Iridium SBD)

- **Iridium Short Burst Data (SBD 9603)**:
  - Max payload per message: **340 Bytes** (Mobile Originated).
  - iTantra's complete ~42-byte iMFP frame fits effortlessly within a single SBD burst, enabling voice-like communication across the Himalayas, open oceans, or remote deserts with zero ground infrastructure.
- **ISRO NavIC Messaging Receiver**:
  - NavIC (IRNSS) broadcast messaging service integrated via RS-232 / BLE bridge for receiving disaster alerts and emergency evacuation coordinates.
