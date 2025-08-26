"use client";

import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { AlertTriangle, X } from "lucide-react";

interface ConfirmDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  type?: "danger" | "warning" | "info";
}

export function ConfirmDialog({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  confirmText = "Confirm",
  cancelText = "Cancel",
  type = "danger"
}: ConfirmDialogProps) {
  const [isConfirming, setIsConfirming] = useState(false);

  const handleConfirm = async () => {
    setIsConfirming(true);
    try {
      await onConfirm();
    } finally {
      setIsConfirming(false);
      onClose();
    }
  };

  const getTypeStyles = () => {
    switch (type) {
      case "danger":
        return {
          icon: "text-red-500",
          button: "bg-red-500 hover:bg-red-600 text-white",
          border: "border-red-200 dark:border-red-800"
        };
      case "warning":
        return {
          icon: "text-yellow-500",
          button: "bg-yellow-500 hover:bg-yellow-600 text-white",
          border: "border-yellow-200 dark:border-yellow-800"
        };
      default:
        return {
          icon: "text-blue-500",
          button: "bg-blue-500 hover:bg-blue-600 text-white",
          border: "border-blue-200 dark:border-blue-800"
        };
    }
  };

  const styles = getTypeStyles();

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50"
            onClick={onClose}
          />
          
          {/* Dialog */}
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 20 }}
            className="fixed inset-0 z-50 flex items-center justify-center p-4"
            onClick={(e) => e.stopPropagation()}
          >
            <div className={`bg-white dark:bg-slate-900 rounded-2xl shadow-2xl border ${styles.border} max-w-md w-full mx-4`}>
              {/* Header */}
              <div className="flex items-center justify-between p-6 border-b border-slate-200/50 dark:border-slate-700/50">
                <div className="flex items-center gap-3">
                  <div className={`p-2 rounded-xl bg-slate-100/50 dark:bg-slate-800/50`}>
                    <AlertTriangle className={`h-5 w-5 ${styles.icon}`} />
                  </div>
                  <h3 className="text-lg font-semibold text-slate-900 dark:text-white">
                    {title}
                  </h3>
                </div>
                <button
                  onClick={onClose}
                  className="p-2 hover:bg-slate-100/50 dark:hover:bg-slate-800/50 rounded-lg transition-colors"
                >
                  <X className="h-4 w-4 text-slate-500" />
                </button>
              </div>
              
              {/* Content */}
              <div className="p-6">
                <div className="text-slate-600 dark:text-slate-400 space-y-3">
                  {message.split('\n').map((line, index) => {
                    if (line.startsWith('⚠️')) {
                      return (
                        <div key={index} className="flex items-start gap-2">
                          <span className="text-yellow-500 text-lg">⚠️</span>
                          <span className="font-medium">{line.replace('⚠️', '').trim()}</span>
                        </div>
                      );
                    } else if (line.startsWith('•')) {
                      return (
                        <div key={index} className="flex items-start gap-2 ml-4">
                          <span className="text-slate-400">•</span>
                          <span>{line.replace('•', '').trim()}</span>
                        </div>
                      );
                    } else if (line.trim() === '') {
                      return <div key={index} className="h-2" />;
                    } else {
                      return <p key={index}>{line}</p>;
                    }
                  })}
                </div>
              </div>
              
              {/* Actions */}
              <div className="flex gap-3 p-6 pt-0">
                <button
                  onClick={onClose}
                  disabled={isConfirming}
                  className="flex-1 px-4 py-2 text-slate-700 dark:text-slate-300 hover:bg-slate-100/50 dark:hover:bg-slate-800/50 rounded-xl transition-colors disabled:opacity-50"
                >
                  {cancelText}
                </button>
                <button
                  onClick={handleConfirm}
                  disabled={isConfirming}
                  className={`flex-1 px-4 py-2 rounded-xl font-medium transition-colors disabled:opacity-50 ${styles.button} shadow-lg hover:shadow-xl`}
                >
                  {isConfirming ? "Deleting..." : confirmText}
                </button>
              </div>
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
}
