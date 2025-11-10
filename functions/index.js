const {setGlobalOptions} = require("firebase-functions/v2");
const stripe = require("stripe")("sk_test_51SPlUkEROr8YYhc1cI6dEoaYhpNc" +
"E6GoQ7dRTezVOltJ5UQ0V29AQN6scWmAPkwEX2Ip5rlhacUw0VgsgpMHfom800RaMESoYV");
const {onSchedule} = require("firebase-functions/v2/scheduler");
const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {onDocumentUpdated} = require("firebase-functions/v2/firestore");
const {onCall} = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

// Inicjalizuj admin tylko raz
admin.initializeApp();

// Ustaw globalne opcje
setGlobalOptions({maxInstances: 10});

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

// ///////////////////////
// Powiadomienie przypomnienie o paczce w zamówieniach
exports.sendParcelReminder = onSchedule("every 1 minutes", async (event) => {
  const db = admin.firestore();
  const now = new Date();
  const localString = now.toLocaleString("en-US", {
    timeZone: "Europe/Warsaw",
  });
  const localNow = new Date(localString);

  const currentDate = localNow.toISOString().split("T")[0]; // "YYYY-MM-DD"
  const currentHour = localNow.getHours();
  const currentMinute = localNow.getMinutes();

  logger.info(
      `Running sendParcelReminder at ${currentHour}:${currentMinute} ` +
    `on ${currentDate}`,
  );

  try {
    const parcelsSnapshot = await db.collection("parcels")
        .where("date", "==", currentDate)
        .get();

    if (parcelsSnapshot.empty) {
      logger.info("No parcels found for today");
      return null;
    }

    for (const parcelDoc of parcelsSnapshot.docs) {
      const parcelData = parcelDoc.data();
      const parcelId = parcelDoc.id;
      const startTime = parcelData.startTime; // np. "14:30"

      if (!startTime) {
        logger.info(`Parcel ${parcelId} has no startTime`);
        continue;
      }

      const [startHour, startMinute] = startTime.split(":").map(Number);

      logger.info(
          `Checking parcel ${parcelId} with startTime ${startHour}:` +
        `${startMinute}`,
      );

      if (startHour === currentHour && startMinute === currentMinute) {
        const usersSnapshot = await db.collection("users")
            .where("orders", "array-contains", parcelId)
            .get();

        if (usersSnapshot.empty) {
          logger.info(
              `No users found with parcel ${parcelId} in orders`,
          );
          continue;
        }

        const notifications = [];
        usersSnapshot.forEach((userDoc) => {
          const userData = userDoc.data();
          const fcmToken = userData.fcmToken;
          if (fcmToken && userData.notificationsEnabled !== false) {
            logger.info(`Adding notification for user ${userDoc.id}`);
            notifications.push({
              token: fcmToken,
              notification: {
                title: "⏰ It's time for your parcel!",
                body: `Your parcel "${parcelData.title || "Untitled"}" ` +
                      `is ready for pickup.`,
              },
              data: {
                type: "parcel_reminder",
                parcelId: parcelId,
              },
              android: {
                notification: {
                  channelId: "parcel_reminder_channel",
                  sound: "default",
                  color: "#FF9800",
                  icon: "ic_launcher",
                },
              },
            });
          } else {
            logger.info(
                `Skipping user ${userDoc.id}: no fcmToken or ` +
              `notifications disabled`,
            );
          }
        });

        if (notifications.length > 0) {
          const results = await admin.messaging().sendEach(notifications);
          logger.info(
              `Sent ${results.successCount} parcel reminders, ` +
            `failed: ${results.failureCount}`,
          );
          results.responses.forEach((res, i) => {
            if (!res.success) {
              logger.error(`Notification ${i} failed:`, res.error);
            }
          });
        } else {
          logger.info("No valid notifications to send for this parcel");
        }
      } else {
        logger.info(`Parcel ${parcelId} not scheduled for this minute`);
      }
    }
  } catch (error) {
    logger.error("Error sending parcel reminders:", error);
  }
});

// /////////////////////////////////////////
// Powiadomienie że ktoś zarezerwował paczkę
exports.notifyOwnerOnParcelReserved = onDocumentUpdated(
    "parcels/{parcelId}",
    async (event) => {
      const beforeData = event.data.before.data();
      const afterData = event.data.after.data();
      const parcelId = event.params.parcelId;

      logger.info(`Checking parcel ${parcelId} for status change`);

      // Sprawdź, czy status zmienił się na "inactive"
      if (beforeData.status !== "inactive" && afterData.status === "inactive") {
        const ownerId = afterData.userId;
        if (!ownerId) {
          logger.error(`Parcel ${parcelId} has no userId`);
          return;
        }

        try {
        // Pobierz dane właściciela
          const ownerDoc = await admin.firestore()
              .collection("users")
              .doc(ownerId)
              .get();

          if (!ownerDoc.exists) {
            logger.error(`Owner ${ownerId} not found`);
            return;
          }

          const ownerData = ownerDoc.data();
          const fcmToken = ownerData.fcmToken;

          if (!fcmToken || ownerData.notificationsEnabled === false) {
            logger.info(`Owner ${ownerId} cannot receive notifications`);
            return;
          }

          logger.info(`Sending notification to `+
          `owner ${ownerId} for parcel ${parcelId}`);

          // Payload powiadomienia
          const payload = {
            token: fcmToken,
            notification: {
              title: "📦 Your parcel was reserved!",
              body: `Someone reserved your parcel ` +
              `"${afterData.title || "Untitled"}"`,
            },
            android: {
              notification: {
                channelId: "parcel_reserved_channel",
                sound: "default",
                color: "#4CAF50",
                icon: "ic_launcher",
              },
            },
            data: {
              type: "parcel_reserved",
              parcelId: parcelId,
            },
          };

          const result = await admin.messaging().send(payload);
          logger.info(`Notification sent to owner ${ownerId}: ${result}`);
        } catch (error) {
          logger.error("Error sending parcel reserved notification:", error);
        }
      } else {
        logger.info(`Parcel ${parcelId} status not changed to inactive`);
      }
    },
);

exports.notifyOwnerOnParcelCancelled = onDocumentUpdated(
    "parcels/{parcelId}",
    async (event) => {
      const beforeData = event.data.before.data();
      const afterData = event.data.after.data();
      const parcelId = event.params.parcelId;

      const beforeStatus = beforeData.status;
      const afterStatus = afterData.status;

      // 📌 1. Sprawdź czy paczka została przywrócona jako "active"
      if (beforeStatus === "inactive" && afterStatus === "active") {
        const ownerId = afterData.userId;
        if (!ownerId) {
          logger.error(`Parcel ${parcelId} has no userId`);
          return;
        }

        try {
          // 🔍 Pobierz dane właściciela
          const ownerDoc = await admin.firestore()
              .collection("users")
              .doc(ownerId)
              .get();

          if (!ownerDoc.exists) {
            logger.error(`Owner ${ownerId} not found`);
            return;
          }

          const ownerData = ownerDoc.data();
          const fcmToken = ownerData.fcmToken;

          if (!fcmToken || ownerData.notificationsEnabled === false) {
            logger.info(`Owner ${ownerId} cannot receive notifications`);
            return;
          }

          logger.info(`Sending cancellation notification to owner ${ownerId}`);

          // 💌 Treść powiadomienia
          const payload = {
            token: fcmToken,
            notification: {
              title: "❗Someone cancelled their reservation",
              body: `Your parcel "${afterData.title || "Untitled"}" ` +
                      `is available again.`,
            },
            android: {
              notification: {
                channelId: "parcel_cancelled_channel",
                sound: "default",
                color: "#FF7043",
                icon: "ic_launcher",
              },
            },
            data: {
              type: "parcel_cancelled",
              parcelId: parcelId,
            },
          };

          // 🚀 Wyślij powiadomienie
          const result = await admin.messaging().send(payload);
          logger.info(`Cancellation notification sent to owner ${ownerId}: ` +
          `${result}`);
        } catch (error) {
          logger.error("Error sending cancellation notification:", error);
        }
      } else {
        logger.info(`Parcel ${parcelId} not reactivated ` +
        `(no cancellation detected)`);
      }
    },
);

// Stripe
exports.createStripeAccount = onCall(async (request) => {
  try {
    // Sprawdź czy użytkownik jest zalogowany
    if (!request.auth) {
      throw new Error("Musisz być zalogowany");
    }

    const userId = request.auth.uid;
    const email = request.data.email;

    // Utwórz konto Stripe Connect Express
    const account = await stripe.accounts.create({
      type: "express",
      country: "PL",
      email: email,
      capabilities: {
        // card_payments: {requested: true},
        transfers: {requested: true},
      },
    });

    // Zapisz account ID w Firebase
    await admin.firestore().collection("users").doc(userId).update({
      stripeAccountId: account.id,
    });

    // Utwórz link do onboardingu
    const accountLink = await stripe.accountLinks.create({
      account: account.id,
      refresh_url: "https://replate-a116a.web.app/stripe-reauth.html",
      return_url: "https://replate-a116a.web.app/stripe-return.html",
      type: "account_onboarding",
    });

    return {
      success: true,
      accountId: account.id,
      onboardingUrl: accountLink.url,
    };
  } catch (error) {
    logger.error("Błąd:", error);
    throw new Error(error.message);
  }
});

// Delete Stripe
exports.deleteStripeAccount = onCall(async (request) => {
  try {
    // Sprawdź czy użytkownik jest zalogowany
    if (!request.auth) {
      throw new Error("Musisz być zalogowany");
    }

    const userId = request.auth.uid;
    const accountId = request.data.accountId;

    if (!accountId) {
      throw new Error("Brak ID konta Stripe");
    }

    // Usuń konto ze Stripe
    await stripe.accounts.del(accountId);

    // Usuń stripeAccountId z Firestore
    await admin.firestore().collection("users").doc(userId).update({
      stripeAccountId: admin.firestore.FieldValue.delete(),
    });

    return {
      success: true,
      message: "Stripe account deleted successfully",
    };
  } catch (error) {
    logger.error("Błąd usuwania konta Stripe:", error);
    throw new Error(error.message);
  }
});

exports.createPaymentIntent = onCall(async (request) => {
  try {
    // Sprawdź czy użytkownik jest zalogowany
    if (!request.auth) {
      throw new Error("Musisz być zalogowany");
    }

    const userId = request.auth.uid;
    const {parcelId, amount} = request.data;

    if (!parcelId || !amount) {
      throw new Error("Brak wymaganych danych");
    }

    // Pobierz dane paczki
    const parcelDoc = await admin.firestore()
        .collection("parcels")
        .doc(parcelId)
        .get();

    if (!parcelDoc.exists) {
      throw new Error("Paczka nie istnieje");
    }

    const parcelData = parcelDoc.data();
    const creatorId = parcelData.userId;

    // Pobierz stripeAccountId twórcy paczki
    const creatorDoc = await admin.firestore()
        .collection("users")
        .doc(creatorId)
        .get();

    if (!creatorDoc.exists) {
      throw new Error("Twórca paczki nie istnieje");
    }

    const stripeAccountId = creatorDoc.data().stripeAccountId;

    if (!stripeAccountId) {
      throw new Error("Twórca nie ma podłączonego konta Stripe");
    }

    // Utwórz Payment Intent z transferem na konto twórcy
    const paymentIntent = await stripe.paymentIntents.create({
      amount: Math.round(amount * 100), // Stripe używa groszy
      currency: "pln",
      transfer_data: {
        destination: stripeAccountId, // Konto twórcy paczki
      },
      metadata: {
        parcelId: parcelId,
        buyerId: userId,
        creatorId: creatorId,
      },
    });

    logger.info(`Payment Intent utworzony: ${paymentIntent.id}`);

    return {
      success: true,
      clientSecret: paymentIntent.client_secret,
      paymentIntentId: paymentIntent.id,
    };
  } catch (error) {
    logger.error("Błąd tworzenia Payment Intent:", error);
    throw new Error(error.message);
  }
});
