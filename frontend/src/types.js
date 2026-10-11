export function pageMeta(page) {
    return page.page ?? {
        size: page.size ?? 10,
        number: page.number ?? 0,
        totalElements: page.totalElements ?? page.content.length,
        totalPages: page.totalPages ?? 1,
    };
}
