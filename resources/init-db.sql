create table if not exists bobouser (
    id       serial primary key, 
    email    varchar unique,
    password varchar
);

create table if not exists token (
    userid    integer references bobouser (id),
    authtoken varchar
);

create table if not exists bookmark (
    userid integer references bobouser (id),
    url    varchar
);

