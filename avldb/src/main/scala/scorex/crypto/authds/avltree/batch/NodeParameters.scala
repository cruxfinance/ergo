package scorex.crypto.authds.avltree.batch

/**
  * Parameters of AVL+ tree nodes (internal and leaves)
  * @param keySize - size of a key (fixed)
  * @param valueSize - size of a value in a leaf (fixed, if defined, arbitrary, if None)
  * @param labelSize - size of a label (node hash), fixed
  */
case class AvlTreeParameters(keySize: Int, valueSize: Option[Int], labelSize: Int) {
  /**
    * @return whether value is fixed-size
    */
  def fixedSizeValue: Boolean = valueSize.isDefined
}
