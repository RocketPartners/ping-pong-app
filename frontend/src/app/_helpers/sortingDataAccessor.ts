const nestedProperty = (data: any, sortHeaderId: string): string | number => {
  return sortHeaderId
    .split('.')
    .reduce((accumulator, key) => accumulator && accumulator[key], data) as
    | string
    | number;
};

const caseInsensitive = (data: any, sortHeaderId: string): string | number => {
  const value = data[sortHeaderId];
  return typeof value === 'string' ? value.toUpperCase() : value;
};

const nestedCaseInsensitive = (
  data: any,
  sortHeaderId: string
): string | number => {
  const value = sortHeaderId
    .split('.')
    .reduce((accumulator, key) => accumulator && accumulator[key], data) as
    | string
    | number;
  return typeof value === 'string' ? value.toUpperCase() : value;
};


const sortingDataAccessor = {
  nestedProperty,
  caseInsensitive,
  nestedCaseInsensitive,
};

export default sortingDataAccessor;
