//[wallet-core](../../../index.md)/[eu.europa.ec.eudi.wallet.presentation](../index.md)/[PresentationManager](index.md)/[rejectRemotePresentation](reject-remote-presentation.md)

# rejectRemotePresentation

[androidJvm]\
abstract fun [rejectRemotePresentation](reject-remote-presentation.md)()

Rejects the current remote presentation request.

This method should be called when the end-user explicitly declines to share their credentials (e.g., by clicking a &quot;Cancel&quot; or &quot;Reject&quot; button in the UI).

It triggers the following actions: Retrieves the cached request details from the current session. Sends a &quot;Negative Consensus&quot; (access_denied) response to the Verifier, ensuring the transaction is closed cleanly on the server side. Clears the internal state and stops the presentation flow.

Note: If no active remote presentation is in progress, this method will log a warning and return safely.
