package org.ergoplatform.nodeView.wallet

import akka.actor.{ActorRef, ActorSystem, Props}
import org.ergoplatform.modifiers.{ErgoFullBlock, ErgoPersistentModifier}
import org.ergoplatform.modifiers.mempool.ErgoTransaction
import org.ergoplatform.nodeView.wallet.ErgoWalletActor._
import org.ergoplatform.settings.ErgoSettings
import scorex.core.VersionTag
import scorex.core.transaction.wallet.Vault
import scorex.core.utils.ScorexLogging

import scala.util.{Failure, Success, Try}
import org.ergoplatform.local.TransactionGenerator.StartGeneration
import org.ergoplatform.local.TransactionGeneratorRef
import org.ergoplatform.nodeView.history.ErgoHistoryReader



class ErgoWallet(actorSystem: ActorSystem,
                 nodeViewHolderRef: ActorRef,
                 historyReader: ErgoHistoryReader,
                 settings: ErgoSettings)
  extends Vault[ErgoTransaction, ErgoPersistentModifier, ErgoWallet] with ErgoWalletReader with ScorexLogging {

  private lazy val seed = settings.walletSettings.seed

  override lazy val actor: ActorRef = actorSystem.actorOf(Props(classOf[ErgoWalletActor], seed))

  implicit val system = actorSystem

  if (settings.testingSettings.transactionGeneration) {
    val txGen = TransactionGeneratorRef(nodeViewHolderRef, actor, settings.testingSettings)
    txGen ! StartGeneration
  }

  def watchFor(address: ErgoAddress): ErgoWallet = {
    actor ! WatchFor(address)
    this
  }

  override def scanOffchain(tx: ErgoTransaction): ErgoWallet = {
    actor ! ScanOffchain(tx)
    this
  }

  override def scanOffchain(txs: Seq[ErgoTransaction]): ErgoWallet = {
    txs.foreach(tx => scanOffchain(tx))
    this
  }

  override def scanPersistent(modifier: ErgoPersistentModifier): ErgoWallet = {
    modifier match {
      case fb: ErgoFullBlock =>
        actor ! ScanOnchain(fb)
      case _ =>
        log.warn("Only a full block is expected in ErgoWallet.scanPersistent")
    }
    this
  }

  override def rollback(to: VersionTag): Try[ErgoWallet] =
    historyReader.heightOf(scorex.core.versionToId(to)) match {
      case Some(height) =>
        actor ! Rollback(height)
        Success(this)
      case None =>
        Failure(new Exception(s"Height of a modifier with id $to not found"))
    }

  override type NVCT = this.type
}


object ErgoWallet {
  def readOrGenerate(actorSystem: ActorSystem,
                     nodeViewHolderRef: ActorRef,
                     historyReader: ErgoHistoryReader,
                     settings: ErgoSettings): ErgoWallet = {
    new ErgoWallet(actorSystem, nodeViewHolderRef, historyReader, settings)
  }
}