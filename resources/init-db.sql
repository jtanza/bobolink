create table if not exists bobouser (
    id       serial primary key, 
    email    varchar not null unique,
    password varchar not null
);

create table if not exists token (
    userid    integer references bobouser (id) not null unique,
    authtoken varchar not null
);

create table if not exists bookmark (
    userid integer references bobouser (id) not null,
    url    varchar not null
);

create index on bookmark (userid);

