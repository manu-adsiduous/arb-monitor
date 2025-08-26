"use client";

interface ToggleProps {
  checked: boolean;
  onChange: (checked: boolean) => void;
  disabled?: boolean;
  className?: string;
}

export function Toggle({ checked, onChange, disabled = false, className = "" }: ToggleProps) {
  const handleToggle = () => {
    if (!disabled) {
      onChange(!checked);
    }
  };

  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      disabled={disabled}
      onClick={handleToggle}
      className={`
        relative inline-flex h-6 w-11 items-center rounded-full transition-all duration-300 ease-in-out
        ${checked 
          ? 'bg-gradient-to-r from-blue-500 to-purple-600 shadow-lg shadow-blue-500/25' 
          : 'bg-slate-200/50 dark:bg-slate-700/50'
        }
        ${disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer hover:scale-105'}
        ${className}
      `}
    >
      <div
        className={`
          pointer-events-none inline-block h-4 w-4 rounded-full bg-white shadow-lg transition-transform duration-300 ease-in-out
          ${checked ? 'translate-x-5' : 'translate-x-1'}
        `}
      >
        {/* Liquid glass effect overlay */}
        <div className="absolute inset-0 rounded-full bg-gradient-to-br from-white/80 to-white/40 backdrop-blur-sm" />
      </div>
      
      {/* Glow effect when checked */}
      {checked && (
        <div className="absolute inset-0 rounded-full bg-gradient-to-r from-blue-400/20 to-purple-400/20" />
      )}
    </button>
  );
}
