package im.actor.core.modules.encryption;

import java.util.ArrayList;

import im.actor.core.api.ApiEncryptedBox;
import im.actor.core.api.ApiEncryptedMessage;
import im.actor.core.api.ApiEncyptedBoxKey;
import im.actor.core.api.ApiMessage;
import im.actor.core.entity.Peer;
import im.actor.core.modules.ModuleContext;
import im.actor.core.modules.encryption.entity.EncryptedBox;
import im.actor.core.modules.encryption.entity.EncryptedBoxKey;
import im.actor.core.util.ModuleActor;
import im.actor.runtime.Log;
import im.actor.runtime.actors.Future;
import im.actor.runtime.actors.ask.AskCallback;

public class MessageEncryptionActor extends ModuleActor {

    private static final String TAG = "MessageEncryptionActor";

    public MessageEncryptionActor(ModuleContext context) {
        super(context);
    }

    private void doEncrypt(int uid, ApiMessage message, final Future future) {
        Log.d(TAG, "doEncrypt");
        ask(context().getEncryption().getEncryptedChatManager(uid), new EncryptedPeerActor.EncryptPackage(message.toByteArray()), new AskCallback() {
            @Override
            public void onResult(Object obj) {
                Log.d(TAG, "doEncrypt:onResult");
                EncryptedBox encryptedBox = (EncryptedBox) obj;
                ArrayList<ApiEncyptedBoxKey> boxKeys = new ArrayList<ApiEncyptedBoxKey>();
                for (EncryptedBoxKey b : encryptedBox.getKeys()) {
                    boxKeys.add(new ApiEncyptedBoxKey(b.getUid(),
                            b.getKeyGroupId(), "curve25519", b.getEncryptedKey()));
                }
                ApiEncryptedBox apiEncryptedBox = new ApiEncryptedBox(boxKeys, "aes-kuznechik", encryptedBox.getEncryptedPackage());
                ApiEncryptedMessage apiEncryptedMessage = new ApiEncryptedMessage(apiEncryptedBox);
                future.onResult(new EncryptedMessage(apiEncryptedMessage));
            }

            @Override
            public void onError(Exception e) {
                Log.d(TAG, "doEncrypt:onError");
                future.onError(e);
            }
        });
    }

    @Override
    public boolean onAsk(Object message, Future future) {
        if (message instanceof EncryptMessage) {
            doEncrypt(((EncryptMessage) message).getUid(), ((EncryptMessage) message).getMessage(),
                    future);
            return false;
        }
        return super.onAsk(message, future);
    }

    public static class InMessage {

        private Peer peer;
        private long date;
        private int senderUid;
        private long rid;
        private ApiEncryptedMessage encryptedMessage;

        public InMessage(Peer peer, long date, int senderUid, long rid, ApiEncryptedMessage encryptedMessage) {
            this.peer = peer;
            this.date = date;
            this.senderUid = senderUid;
            this.rid = rid;
            this.encryptedMessage = encryptedMessage;
        }
    }

    public static class EncryptMessage {

        private int uid;
        private ApiMessage message;

        public EncryptMessage(int uid, ApiMessage message) {
            this.uid = uid;
            this.message = message;
        }

        public int getUid() {
            return uid;
        }

        public ApiMessage getMessage() {
            return message;
        }
    }

    public static class EncryptedMessage {
        private ApiEncryptedMessage encryptedMessage;

        public EncryptedMessage(ApiEncryptedMessage encryptedMessage) {
            this.encryptedMessage = encryptedMessage;
        }

        public ApiEncryptedMessage getEncryptedMessage() {
            return encryptedMessage;
        }
    }
}
