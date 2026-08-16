import { motion } from 'framer-motion';

const Input = ({
  label,
  type = 'text',
  name,
  value,
  onChange,
  placeholder,
  error,
  className = '',
  required = false,
  ...props
}) => {
  return (
    <motion.div 
      className={`mb-4 ${className}`}
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
    >
      {label && (
        <label htmlFor={name} className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
          {label} {required && <span className="text-red-500">*</span>}
        </label>
      )}
      <input
        type={type}
        id={name}
        name={name}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        className={`input w-full ${error ? 'border-red-500 focus:ring-red-500' : ''}`}
        required={required}
        {...props}
      />
      {error && <p className="mt-1 text-sm text-red-500">{error}</p>}
    </motion.div>
  );
};

export default Input;