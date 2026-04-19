const express = require('express');
const nodemailer = require('nodemailer');
const bodyParser = require('body-parser');
const cors = require('cors');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3001;

// Middleware
app.use(cors());
app.use(bodyParser.json());

// Create transporter
const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST,
  port: process.env.SMTP_PORT,
  secure: false, // true for 465, false for other ports
  auth: {
    user: process.env.SMTP_USER,
    pass: process.env.SMTP_PASS,
  },
});

// Verify transporter
transporter.verify((error, success) => {
  if (error) {
    console.error('Email service error:', error);
  } else {
    console.log('Email service is ready to send messages');
  }
});

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'UP', service: 'email-service' });
});

// Send email endpoint
app.post('/api/email/send', async (req, res) => {
  try {
    const { to, subject, text, html, taskId } = req.body;

    if (!to || !subject) {
      return res.status(400).json({ error: 'Missing required fields: to, subject' });
    }

    // Build email with task link if taskId provided
    let emailHtml = html;
    let emailText = text;

    if (taskId) {
      const taskUrl = `${process.env.APP_URL}/tasks/${taskId}`;
      emailHtml = `
        ${html || text}
        <br><br>
        <a href="${taskUrl}" style="display: inline-block; padding: 10px 20px; background-color: #3B82F6; color: white; text-decoration: none; border-radius: 5px;">
          Перейти к задаче
        </a>
      `;
      emailText = `${text}\n\nПерейти к задаче: ${taskUrl}`;
    }

    const mailOptions = {
      from: `"${process.env.FROM_NAME}" <${process.env.FROM_EMAIL}>`,
      to,
      subject,
      text: emailText,
      html: emailHtml,
    };

    const info = await transporter.sendMail(mailOptions);
    
    console.log('Email sent:', info.messageId);
    res.json({ 
      success: true, 
      messageId: info.messageId,
      message: 'Email sent successfully' 
    });
  } catch (error) {
    console.error('Error sending email:', error);
    res.status(500).json({ 
      error: 'Failed to send email',
      details: error.message 
    });
  }
});

// Send notification email (specific endpoint for notifications)
app.post('/api/email/notification', async (req, res) => {
  try {
    const { userEmail, userName, message, type, taskId } = req.body;

    if (!userEmail || !message) {
      return res.status(400).json({ error: 'Missing required fields: userEmail, message' });
    }

    const taskUrl = taskId ? `${process.env.APP_URL}/tasks/${taskId}` : null;

    const subject = getSubjectByType(type);
    const htmlContent = `
      <!DOCTYPE html>
      <html>
      <head>
        <style>
          body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
          .container { max-width: 600px; margin: 0 auto; padding: 20px; }
          .header { background-color: #3B82F6; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
          .content { background-color: #f9fafb; padding: 30px; border: 1px solid #e5e7eb; }
          .message { background-color: white; padding: 20px; border-radius: 5px; margin: 20px 0; }
          .button { display: inline-block; padding: 12px 24px; background-color: #3B82F6; color: white; text-decoration: none; border-radius: 5px; margin-top: 20px; }
          .footer { text-align: center; color: #6b7280; font-size: 12px; margin-top: 20px; }
        </style>
      </head>
      <body>
        <div class="container">
          <div class="header">
            <h1>Sprint Approve</h1>
          </div>
          <div class="content">
            <p>Здравствуйте${userName ? ', ' + userName : ''}!</p>
            <div class="message">
              <p><strong>${message}</strong></p>
            </div>
            ${taskUrl ? `
              <p>Вы можете перейти к задаче по ссылке ниже:</p>
              <a href="${taskUrl}" class="button">Перейти к задаче</a>
            ` : ''}
          </div>
          <div class="footer">
            <p>Это автоматическое уведомление из системы Sprint Approve</p>
          </div>
        </div>
      </body>
      </html>
    `;

    const mailOptions = {
      from: `"${process.env.FROM_NAME}" <${process.env.FROM_EMAIL}>`,
      to: userEmail,
      subject,
      html: htmlContent,
      text: `${message}${taskUrl ? '\n\nПерейти к задаче: ' + taskUrl : ''}`,
    };

    const info = await transporter.sendMail(mailOptions);
    
    console.log('Notification email sent to:', userEmail);
    res.json({ 
      success: true, 
      messageId: info.messageId 
    });
  } catch (error) {
    console.error('Error sending notification email:', error);
    res.status(500).json({ 
      error: 'Failed to send notification email',
      details: error.message 
    });
  }
});

function getSubjectByType(type) {
  const subjects = {
    'TASK_ASSIGNED': '📋 Вам назначена новая задача',
    'TASK_SUBMITTED_FOR_REVIEW': '👀 Задача отправлена на рассмотрение',
    'TASK_APPROVED': '✅ Задача одобрена',
    'TASK_REJECTED': '❌ Задача отклонена',
  };
  return subjects[type] || '📬 Новое уведомление';
}

app.listen(PORT, () => {
  console.log(`Email service running on port ${PORT}`);
});
