# Kanjilink: מה עושים ידנית ב-Play Console ו-GitHub

כל השאר (קוד, חתימה, דף חנות, תמונות, מסמכים משפטיים) מוכן בריפו. הצעדים כאן הם מה ש-Google ו-GitHub מאפשרים רק לבעלים.

## א. GitHub Secrets (ריפו kanjilink)
GitHub ← ronmailx-boop/kanjilink ← Settings ← Secrets and variables ← Actions ← New repository secret:
1. ארבעת הערכים מהקובץ `kanjilink-upload-key-SECRETS.txt`: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
2. `CLOUDFLARE_API_TOKEN`: מפתח חדש ב-Cloudflare ← My Profile ← API Tokens ← Create Token ← תבנית **Edit Cloudflare Workers** ← Account Resources: החשבון ← Zone Resources: Specific zone ← `vplusstudio.app` ← שם `kanjilink-deploy`.

## ב. יצירת האפליקציה ב-Play Console
Create app:
- App name: **Kanjilink: Kanji Lock Screen**
- Default language: **English (United States) – en-US**
- App or game: **App** · Free or paid: **Free** (התשלום הוא רכישה בתוך האפליקציה)
- לאשר את ההצהרות (Developer Program Policies, US export laws).

## ג. העלאה ראשונה (ידנית, פעם אחת)
1. GitHub ← Actions ← "Android build" ← הריצה האחרונה ב-main ← להוריד את `kanjilink-release-aab` (קיים רק אחרי שהוספת את ה-Secrets מסעיף א).
2. Play Console ← Test and release ← Testing ← **Internal testing** ← Create new release ← להעלות את `app-release.aab`.
3. Play App Signing: לאשר (ברירת המחדל).
4. לשמור כטיוטה. אחרי זה אני מעלה גרסאות ודף חנות דרך ה-API.

## ד. הרשאה לחשבון השירות (כדי שאוכל לעדכן את דף החנות)
Users and permissions ← `agefox-play@red-grid-513513-t6.iam.gserviceaccount.com` ← App permissions ← Kanjilink ← לסמן **Edit and delete draft apps** (בלי זה כל שינוי בדף החנות מחזיר 403 עד שהאפליקציה מפורסמת).

## ה. מוצר Pro
Monetize ← Products ← **In-app products** ← Create product:
- Product ID: **`kanjilink_pro`** (בדיוק כך, אי אפשר לשנות)
- Name: **Kanjilink Pro**
- Description: **Unlocks JLPT N4, N3, N2 and N1: all 2,211 kanji. One payment, no subscription.**
- Price: ‏[המחיר שבחרת] ← Activate.
(האפשרות מופיעה רק אחרי ההעלאה הראשונה מסעיף ג, כי האפליקציה כוללת את ספריית התשלומים.)

## ו. App content (טפסים)
| טופס | מה לענות |
|---|---|
| Privacy policy | `https://kanjilink.vplusstudio.app/privacy` |
| App access | All functionality is available without special access |
| Ads | No, my app does not contain ads |
| Content rating | Category: Education/Reference · אין אלימות, מין, שפה, הימורים, תוכן משתמשים או שיתוף מיקום · רכישות דיגיטליות: Yes |
| Target audience | 13–15, 16–17, 18 ומעלה (לא מתחת ל-13, כדי לא להיכנס למדיניות Families) · Appeal to children: No |
| Data safety | Does your app collect or share user data? **No** (הכול נשמר במכשיר; רכישות מטופלות על ידי Google Play) |
| Government apps / Financial features / Health | No / None / No |
| News app | No |

## ז. Store settings
- App category: **Education** · Tags: Education, Language learning
- Contact email: vplusstudio.app@gmail.com · Website: `https://kanjilink.vplusstudio.app`

## ח. אחרי זה (אני)
דף החנות באנגלית, 6 צילומי מסך, תמונת נושא ואייקון מ-`store/en-US/` דרך ה-API (תראה ותאשר לפני), ואז גרסה ב-Internal testing. הפרסום ל-Production נשאר אצלך.
