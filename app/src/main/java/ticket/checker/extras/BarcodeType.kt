package ticket.checker.extras

import com.google.android.gms.vision.barcode.Barcode
import java.io.Serializable

enum class BarcodeType(val format : String, val id : Int) : Serializable {
    ALL_FORMATS("All Formats", Barcode.ALL_FORMATS),
    EAN_13("EAN-13", Barcode.EAN_13),
    EAN_8("EAN-8", Barcode.EAN_8),
    UPC_A("UPC-A", Barcode.UPC_A),
    UPC_E("UPC-E", Barcode.UPC_E),
    CODE_128("Code-128", Barcode.CODE_128),
    CODE_93("Code-93", Barcode.CODE_93),
    CODE_39("Code-39", Barcode.CODE_39),
    ITF("ITF", Barcode.ITF),
    CODABAR("Codabar", Barcode.CODABAR),
    QR_CODE("QR Code", Barcode.QR_CODE),
    DATA_MATRIX("Data Matrix", Barcode.DATA_MATRIX),
    PDF417("PDF-417", Barcode.PDF417),
    AZTEC("AZTEC", Barcode.AZTEC);

    companion object {
        fun fromFormatToBarcodeType(format : String) : BarcodeType {
            return BarcodeType.values().find { it.format == format } ?: ALL_FORMATS
        }
        fun fromIdToBarcodeType(id : Int) : BarcodeType {
            return BarcodeType.values().find { it.id == id } ?: ALL_FORMATS
        }
    }
}