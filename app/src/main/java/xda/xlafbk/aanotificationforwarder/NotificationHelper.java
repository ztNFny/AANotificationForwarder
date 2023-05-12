package xda.xlafbk.aanotificationforwarder;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;
import androidx.preference.PreferenceManager;

public class NotificationHelper {

    @SuppressLint("MissingPermission")
    public static void sendCarNotification(Context context, String title, String message, @Nullable String groupTitle, Bitmap largeIcon, int conversationId) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int settingClearTimeout = preferences.getInt("dismissInterval", 15);

        // Dummy Mark Read and Reply intents required for Android Auto
        PendingIntent msgReplyPendingIntent = PendingIntent.getBroadcast(context, conversationId, new Intent(), PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(R.mipmap.ic_launcher, "Reply", msgReplyPendingIntent)
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                .setShowsUserInterface(false)
                .addRemoteInput(new RemoteInput.Builder("").build())
                .build();

        PendingIntent msgReadPendingIntent = PendingIntent.getBroadcast(context, conversationId, new Intent(), PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Action markAsReadAction = new NotificationCompat.Action.Builder(R.mipmap.ic_launcher, "Mark as Read", msgReadPendingIntent)
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
                .setShowsUserInterface(false)
                .build();

        Person user = new Person.Builder().setName("Dummy - not used").build();
        NotificationCompat.MessagingStyle style;
        if (groupTitle != null) {
            style = new NotificationCompat.MessagingStyle(user)
                .addMessage(message, System.currentTimeMillis(), new Person.Builder().setName(title).build())
                .setConversationTitle(groupTitle) // Optional: If given this will be shown instead of the Person as heading
                .setGroupConversation(true); // true = Prefix message with sender name
        } else {
            style = new NotificationCompat.MessagingStyle(user)
                .addMessage(message, System.currentTimeMillis(), new Person.Builder().setName(title).build())
                .setGroupConversation(false); // true = Prefix message with sender name
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, MainActivity.channelIdImportant)
                .setLargeIcon(largeIcon)
                .setSmallIcon(R.drawable.icon_small)
                .addInvisibleAction(replyAction)
                .addInvisibleAction(markAsReadAction)
                .setStyle(style);

        if (settingClearTimeout > 0) {
            mBuilder.setTimeoutAfter(settingClearTimeout * 1000L);
        }

        NotificationManagerCompat msgNotificationManager = NotificationManagerCompat.from(context);
        msgNotificationManager.notify(conversationId, mBuilder.build());

    }

    // Get the best notification icon (large, small, default) and return it as bitmap
    public static Bitmap getNotificationIconBitmap(Context context, Notification notification) {
        Bitmap bmp = null;
        Icon icon = notification.getLargeIcon();
        if (icon == null) {
            icon = notification.getSmallIcon();
        }
        if (icon != null) {
            bmp = drawableToBitMap(icon.loadDrawable(context));
        }
        if (bmp == null) {
            bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_small);
        }
        return bmp;
    }

    public static Bitmap drawableToBitMap(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = ((BitmapDrawable) drawable);
            return bitmapDrawable.getBitmap();
        } else {
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                    drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            drawable.draw(canvas);
            return bitmap;
        }
    }
}
