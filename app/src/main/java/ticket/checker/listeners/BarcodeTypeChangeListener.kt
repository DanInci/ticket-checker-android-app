package ticket.checker.listeners

import ticket.checker.extras.BarcodeType

interface BarcodeTypeChangeListener {
    fun onBarcodeTypeChanged(barcodeType : BarcodeType)
}