const {setGlobalOptions} = require("firebase-functions/v2");
const {onRequest} = require("firebase-functions/v2/https");
const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

// Inicjalizuj admin tylko raz
admin.initializeApp();

// Ustaw globalne opcje
setGlobalOptions({maxInstances: 10});

exports.sendLoginNotification = onRequest(async (req, res) => {
  // CORS (opcjonalnie, jeśli potrzebujesz)
  res.set("Access-Control-Allow-Origin", "*");

  if (req.method === "OPTIONS") {
    res.set("Access-Control-Allow-Methods", "POST");
    res.set("Access-Control-Allow-Headers", "Content-Type");
    return res.status(204).send("");
  }

  const {fcmToken, message} = req.body;

  // Walidacja
  if (!fcmToken || !message) {
    logger.error("Missing fcmToken or message");
    return res.status(400).send("Missing fcmToken or message");
  }

  // Payload powiadomienia
  const payload = {
    notification: {
      title: "Nowe logowanie!",
      body: message,
    },
  };

  try {
    // Wyślij powiadomienie
    await admin.messaging().send({
      token: fcmToken,
      notification: payload.notification,
    });

    logger.info("Notification sent successfully to:", fcmToken);
    res.status(200).send("Notification sent!");
  } catch (error) {
    logger.error("Error sending notification:", error);
    res.status(500).send("Error: " + error.message);
  }
});

exports.notifyFavoritesOnNewParcel = onDocumentCreated(
    "parcels/{parcelId}",
    async (event) => {
      const parcelData = event.data.data();
      const parcelId = event.params.parcelId;

      logger.info("New parcel created:", parcelId, parcelData);

      // Pobierz userId właściciela paczki
      const ownerId = parcelData.userId;

      if (!ownerId) {
        logger.error("Parcel has no userId");
        return;
      }

      try {
      // Znajdź użytkownika (właściciela paczki)
        const ownerDoc = await admin.firestore()
            .collection("users")
            .doc(ownerId)
            .get();

        if (!ownerDoc.exists) {
          logger.error("Owner not found:", ownerId);
          return;
        }

        const ownerName = ownerDoc.data().login || "Użytkownik";

        // Znajdź wszystkich użytkowników, którzy mają tego użytkownika w ulubio
        const usersSnapshot = await admin.firestore()
            .collection("users")
            .where("likedUsers", "array-contains", ownerId)
            .get();

        if (usersSnapshot.empty) {
          logger.info("No users have this owner in favorites");
          return;
        }

        logger.info(
            `Found ${usersSnapshot.size} users ` +
          `with this owner in favorites`,
        );

        // Przygotuj powiadomienia
        const notifications = [];

        usersSnapshot.forEach((userDoc) => {
          const userData = userDoc.data();
          const fcmToken = userData.fcmToken;
          const notificationsEnabled = userData.notificationsEnabled !== false;
          const parcelTitle = parcelData.title || "bez tytułu";

          if (fcmToken && notificationsEnabled) {
            logger.info("Preparing notification for:", userDoc.id);

            notifications.push({
              token: fcmToken,
              notification: {
                title: `🎉 New parcel from ${ownerName}!`,
                body: `${parcelTitle} is waiting for you!`,
                // icon: "https://link_do_ikony_replate.png", // opcjonalnie własna ikona online
              },
              android: { // specyficzne ustawienia dla Androida
                notification: {
                  channelId: "new_parcel_channel",
                  sound: "default", // systemowy dźwięk
                  color: "#FF5722", // fajny kolor dla ikony w tray
                  icon: "ic_launcher", // lokalna ikona w drawable
                },
              },
              data: {
                type: "new_parcel",
                parcelId: parcelId,
                ownerId: ownerId,
              },
            });
          }
        });

        if (notifications.length === 0) {
          logger.info("No valid FCM tokens found");
          return;
        }

        // Wyślij wszystkie powiadomienia
        const results = await admin.messaging().sendEach(notifications);

        logger.info(`Sent ${results.successCount} notifications successfully`);
        logger.error(`Failed to send ${results.failureCount} notifications`);

        // 🔍 DODANY BLOK LOGOWANIA — tu zobaczysz, czemu nie wysyła
        results.responses.forEach((res, i) => {
          if (!res.success) {
            logger.error(`Notification ${i} failed:`, res.error);
          }
        });
      } catch (error) {
        logger.error("Error sending notifications:", error);
      }
    },
);
