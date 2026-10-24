# Sale barcode scanner

Use the Scan button beside the sale product search:

- Camera reads printed product barcodes with this device's camera.
- Mobile displays a temporary pairing QR and link. Open the link with a phone,
  tap Start Camera, and scan a product. The sale screen receives the barcode,
  selects the product and focuses Quantity. Add the product before scanning
  the next one. Closing the modal keeps the phone connected; Disconnect ends it.

Product scanning accepts only linear barcodes (including Code 128, Code 39,
EAN and UPC). QR codes are used only to connect the phone, not identify products.

The scanner matches active products by exact barcode. Leading zeroes are
preserved. Customer pricing and warehouse selection use the normal sale flow.
Scanning does not create drafts, add lines, change stock, or complete a sale.

## Phone access

Serve the app through HTTPS with a certificate trusted by the phone, and open
the sale page at that same address on the laptop. Both devices must be able to
reach it. A localhost link refers to the phone itself and will not reach the
laptop. HTTP on a LAN IP does not provide browser camera access. Manual barcode
entry on the phone remains available if camera permission is denied.

Pairing links expire after 10 minutes and grant only the ability to send
barcode text. Reading scans and disconnecting require the originating HTTP
session. Connections are held in memory and end on server restart. Use one
application instance (or session affinity); multiple instances need shared
pairing storage. Only one phone connection per browser session is supported.

## Bundled libraries

- html5-qrcode 2.3.8: https://github.com/mebjas/html5-qrcode (Apache-2.0).
- QRCode.js 1.0.0: https://github.com/davidshimjs/qrcodejs (MIT).

Scripts are served locally from static/resources/js; no CDN is contacted during
scanning. Camera permission is requested only after the user chooses a camera.
