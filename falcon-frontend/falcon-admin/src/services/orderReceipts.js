import api from './api';

const fallbackFileName = (orderId) => `receipt-${orderId}.pdf`;

const parseFileName = (contentDisposition, orderId) => {
  if (!contentDisposition) {
    return fallbackFileName(orderId);
  }

  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1]);
  }

  const asciiMatch = contentDisposition.match(/filename="?([^"]+)"?/i);
  if (asciiMatch?.[1]) {
    return asciiMatch[1];
  }

  return fallbackFileName(orderId);
};

const downloadBlob = (blob, fileName) => {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => window.URL.revokeObjectURL(url), 1000);
};

const extractErrorMessage = async (error, fallbackMessage) => {
  const responseData = error?.response?.data;

  if (responseData instanceof Blob) {
    try {
      const text = await responseData.text();
      if (!text) {
        return fallbackMessage;
      }

      try {
        const parsed = JSON.parse(text);
        return parsed.message || parsed.error || fallbackMessage;
      } catch (_) {
        return text;
      }
    } catch (_) {
      return fallbackMessage;
    }
  }

  return (
    responseData?.message ||
    responseData?.error ||
    error?.message ||
    fallbackMessage
  );
};

export const downloadOrderReceipt = async (orderId) => {
  try {
    const response = await api.get(`/api/orders/${orderId}/receipt`, {
      responseType: 'blob',
    });

    const fileName = parseFileName(
      response.headers['content-disposition'],
      orderId,
    );

    downloadBlob(response.data, fileName);
    return fileName;
  } catch (error) {
    const message = await extractErrorMessage(
      error,
      'Failed to download receipt.',
    );
    throw new Error(message);
  }
};
