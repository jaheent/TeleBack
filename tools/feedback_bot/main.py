import os
import telebot
from telebot.types import InlineKeyboardMarkup, InlineKeyboardButton
from datetime import datetime

# Environment variables for zero-secret deployment
# NEVER hardcode tokens here. Set BOT_TOKEN and GROUP_ID in your hosting dashboard or .env file.
BOT_TOKEN = os.getenv("BOT_TOKEN")
if not BOT_TOKEN:
    raise ValueError("Missing environment variable: BOT_TOKEN must be set.")

GROUP_ID_STR = os.getenv("GROUP_ID")
if not GROUP_ID_STR:
    raise ValueError("Missing environment variable: GROUP_ID must be set.")
GROUP_ID = int(GROUP_ID_STR)

bot = telebot.TeleBot(BOT_TOKEN)

def build_ticket_markup(status="unreviewed"):
    markup = InlineKeyboardMarkup(row_width=3)
    if status == "unreviewed":
        markup.add(
            InlineKeyboardButton("✅ Review", callback_data="review"),
            InlineKeyboardButton("📁 Archive", callback_data="archive"),
            InlineKeyboardButton("🗑 Discard", callback_data="discard")
        )
    elif status == "reviewed":
        markup.add(
            InlineKeyboardButton("🔄 Reopen", callback_data="reopen"),
            InlineKeyboardButton("📁 Archive", callback_data="archive"),
            InlineKeyboardButton("🗑 Discard", callback_data="discard")
        )
    return markup

@bot.message_handler(commands=['start', 'help'])
def send_welcome(message):
    welcome_text = (
        "👋 *Welcome to TeleBack Feedback & Diagnostics Bot!*\n\n"
        "To submit a report, simply paste your copied crash dump or type your feedback below.\n"
        "It will be forwarded directly to the developer team with zero-knowledge privacy."
    )
    bot.reply_to(message, welcome_text, parse_mode="Markdown")

@bot.message_handler(func=lambda msg: True, content_types=['text', 'document'])
def handle_user_submission(message):
    # Only forward direct private messages sent to the bot by users
    if message.chat.type != "private":
        return

    user_info = f"@{message.from_user.username}" if message.from_user.username else f"User {message.from_user.id}"
    header = (
        f"📩 *NEW TICKET* from {user_info}\n"
        f"━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
        f"Status: 🔴 *UNREVIEWED*\n\n"
    )

    report_content = message.text or (message.caption or "[Document attached]")
    full_card = header + report_content

    # If document attached, forward document; otherwise send formatted markdown message
    if message.document:
        bot.send_document(
            chat_id=GROUP_ID,
            document=message.document.file_id,
            caption=full_card[:1024],
            reply_markup=build_ticket_markup("unreviewed"),
            parse_mode="Markdown"
        )
    else:
        bot.send_message(
            chat_id=GROUP_ID,
            text=full_card[:4096],
            reply_markup=build_ticket_markup("unreviewed"),
            parse_mode="Markdown"
        )

    bot.reply_to(message, "✅ *Ticket received!* The TeleBack developer team has been notified.", parse_mode="Markdown")

@bot.callback_query_handler(func=lambda call: True)
def handle_ticket_actions(call):
    admin_name = f"@{call.from_user.username}" if call.from_user.username else call.from_user.first_name
    timestamp = datetime.utcnow().strftime("%Y-%m-%d %H:%M UTC")
    current_text = call.message.text or (call.message.caption or "")

    if call.data == "review":
        updated_text = current_text.replace("🔴 *UNREVIEWED*", f"🟢 *REVIEWED* (by {admin_name} at {timestamp})")
        updated_text = updated_text.replace("🔴 UNREVIEWED", f"🟢 REVIEWED (by {admin_name} at {timestamp})")
        
        if call.message.text:
            bot.edit_message_text(
                chat_id=call.message.chat.id,
                message_id=call.message.message_id,
                text=updated_text,
                reply_markup=build_ticket_markup("reviewed"),
                parse_mode="Markdown"
            )
        else:
            bot.edit_message_caption(
                chat_id=call.message.chat.id,
                message_id=call.message.message_id,
                caption=updated_text[:1024],
                reply_markup=build_ticket_markup("reviewed"),
                parse_mode="Markdown"
            )
        bot.answer_callback_query(call.id, "Ticket marked as Reviewed! 🟢")

    elif call.data == "reopen":
        updated_text = current_text
        if "🟢 *REVIEWED*" in updated_text:
            updated_text = updated_text.replace("🟢 *REVIEWED*", "🔴 *UNREVIEWED*")
        elif "🟢 REVIEWED" in updated_text:
            updated_text = updated_text.replace("🟢 REVIEWED", "🔴 UNREVIEWED")

        if call.message.text:
            bot.edit_message_text(
                chat_id=call.message.chat.id,
                message_id=call.message.message_id,
                text=updated_text,
                reply_markup=build_ticket_markup("unreviewed"),
                parse_mode="Markdown"
            )
        else:
            bot.edit_message_caption(
                chat_id=call.message.chat.id,
                message_id=call.message.message_id,
                caption=updated_text[:1024],
                reply_markup=build_ticket_markup("unreviewed"),
                parse_mode="Markdown"
            )
        bot.answer_callback_query(call.id, "Ticket Reopened 🔴")

    elif call.data == "archive":
        archived_text = current_text
        archived_text = archived_text.replace("🔴 *UNREVIEWED*", f"📦 *ARCHIVED* (by {admin_name})")
        archived_text = archived_text.replace("🔴 UNREVIEWED", f"📦 ARCHIVED (by {admin_name})")
        archived_text = archived_text.replace("🟢 *REVIEWED*", f"📦 *ARCHIVED* (by {admin_name})")
        archived_text = archived_text.replace("🟢 REVIEWED", f"📦 ARCHIVED (by {admin_name})")

        if call.message.text:
            bot.edit_message_text(
                chat_id=call.message.chat.id,
                message_id=call.message.message_id,
                text=archived_text,
                reply_markup=None,  # Finalized state: remove buttons
                parse_mode="Markdown"
            )
        else:
            bot.edit_message_caption(
                chat_id=call.message.chat.id,
                message_id=call.message.message_id,
                caption=archived_text[:1024],
                reply_markup=None,
                parse_mode="Markdown"
            )
        bot.answer_callback_query(call.id, "Ticket Archived 📦")

    elif call.data == "discard":
        bot.delete_message(chat_id=call.message.chat.id, message_id=call.message.message_id)
        bot.answer_callback_query(call.id, "Ticket Discarded 🗑")

if __name__ == "__main__":
    print("TeleBack Feedback Bot is listening...")
    bot.infinity_polling()
