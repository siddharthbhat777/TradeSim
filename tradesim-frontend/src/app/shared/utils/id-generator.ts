let globalId = 0;

export const generateUniqueId = (prefix: string = 'trade-sim'): string => {
    return `${prefix}-${globalId++}`;
};