import { HomeItemsResponse } from '../types/HomeItemsResponse';

const itemsByUser = new Map<number, { itemsUrl: string; items: HomeItemsResponse }>();
const tagsByUser = new Map<number, string[]>();

const copyHomeItems = (items: HomeItemsResponse): HomeItemsResponse => ({
  tasks: items.tasks.map(task => ({ ...task, tags: [...task.tags], urls: [...task.urls] })),
  notes: items.notes.map(note => ({ ...note, tags: [...note.tags] }))
});

/**
 * Return the last home response for this user and filter, if one is available.
 *
 * @param {number} userId - The signed-in user's ID.
 * @param {string} itemsUrl - The URL identifying the current filter.
 * @returns {HomeItemsResponse | undefined} A cached response, if available.
 */
export const getCachedHomeItems = (
  userId: number,
  itemsUrl: string
): HomeItemsResponse | undefined => {
  const cached = itemsByUser.get(userId);
  return cached?.itemsUrl === itemsUrl
    ? copyHomeItems(cached.items)
    : undefined;
};

/**
 * Store the latest home response for this user and filter.
 *
 * @param {number} userId - The signed-in user's ID.
 * @param {string} itemsUrl - The URL identifying the current filter.
 * @param {HomeItemsResponse} items - The response returned by the home API.
 */
export const cacheHomeItems = (
  userId: number,
  itemsUrl: string,
  items: HomeItemsResponse
): void => {
  itemsByUser.set(userId, {
    itemsUrl,
    items: copyHomeItems(items)
  });
};

/**
 * Return the last tag list loaded for this user, if one is available.
 *
 * @param {number} userId - The signed-in user's ID.
 * @returns {string[] | undefined} A cached tag list, if available.
 */
export const getCachedHomeTags = (userId: number): string[] | undefined => {
  const tags = tagsByUser.get(userId);
  return tags ? [...tags] : undefined;
};

/**
 * Store the latest tag list loaded for this user.
 *
 * @param {number} userId - The signed-in user's ID.
 * @param {string[]} tags - The tags returned by the home API.
 */
export const cacheHomeTags = (userId: number, tags: string[]): void => {
  tagsByUser.set(userId, [...tags]);
};

/**
 * Clear the in-memory home data cache.
 *
 * This is exported so tests can start with an empty cache.
 */
export const clearHomeCache = (): void => {
  itemsByUser.clear();
  tagsByUser.clear();
};
