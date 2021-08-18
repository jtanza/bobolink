create table if not exists bobouser (
    id       integer primary key,
    email    varchar not null unique,
    password varchar not null
);

create table if not exists token (
    userid    integer references bobouser (id) not null unique,
    authtoken varchar not null
);

create table if not exists bookmark (
    userid integer references bobouser (id) not null,
    url    varchar not null,
    unique (userid, url)
);

create table if not exists reset_token (
    userid  integer references bobouser (id) not null unique,
    token   varchar not null,
    expires timestamp not null
);


create index bmark_index on bookmark (userid);
