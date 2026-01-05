--
-- PostgreSQL database dump
--

\restrict 0lMWG6b1mAwffy9Jxj9gheFjCC81RcuHIlPwec8lGrbJ8poBFXuhgzTBGh92bni

-- Dumped from database version 16.11
-- Dumped by pg_dump version 16.11

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: br; Type: SCHEMA; Schema: -; Owner: br_user
--

CREATE SCHEMA br;


ALTER SCHEMA br OWNER TO br_user;

--
-- Name: pg_trgm; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;


--
-- Name: EXTENSION pg_trgm; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION pg_trgm IS 'text similarity measurement and index searching based on trigrams';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: consigli_libri; Type: TABLE; Schema: br; Owner: br_user
--

CREATE TABLE br.consigli_libri (
    userid text NOT NULL,
    libro_id integer NOT NULL,
    suggerito_id integer NOT NULL
);


ALTER TABLE br.consigli_libri OWNER TO br_user;

--
-- Name: librerie; Type: TABLE; Schema: br; Owner: br_user
--

CREATE TABLE br.librerie (
    userid text NOT NULL,
    nome text NOT NULL
);


ALTER TABLE br.librerie OWNER TO br_user;

--
-- Name: librerie_libri; Type: TABLE; Schema: br; Owner: br_user
--

CREATE TABLE br.librerie_libri (
    userid text NOT NULL,
    nome text NOT NULL,
    libro_id integer NOT NULL
);


ALTER TABLE br.librerie_libri OWNER TO br_user;

--
-- Name: libri; Type: TABLE; Schema: br; Owner: br_user
--

CREATE TABLE br.libri (
    id integer NOT NULL,
    titolo text NOT NULL,
    anno integer,
    editore text,
    categoria text
);


ALTER TABLE br.libri OWNER TO br_user;

--
-- Name: libri_autori; Type: TABLE; Schema: br; Owner: br_user
--

CREATE TABLE br.libri_autori (
    libro_id integer NOT NULL,
    autore text NOT NULL
);


ALTER TABLE br.libri_autori OWNER TO br_user;

--
-- Name: libri_id_seq; Type: SEQUENCE; Schema: br; Owner: br_user
--

CREATE SEQUENCE br.libri_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE br.libri_id_seq OWNER TO br_user;

--
-- Name: libri_id_seq; Type: SEQUENCE OWNED BY; Schema: br; Owner: br_user
--

ALTER SEQUENCE br.libri_id_seq OWNED BY br.libri.id;


--
-- Name: utenti_registrati; Type: TABLE; Schema: br; Owner: br_user
--

CREATE TABLE br.utenti_registrati (
    userid text NOT NULL,
    password_hash text NOT NULL,
    nome text NOT NULL,
    cognome text NOT NULL,
    codice_fiscale text NOT NULL,
    email text NOT NULL,
    CONSTRAINT chk_pwdhash_len CHECK ((length(password_hash) = 64))
);


ALTER TABLE br.utenti_registrati OWNER TO br_user;

--
-- Name: valutazioni_libri; Type: TABLE; Schema: br; Owner: br_user
--

CREATE TABLE br.valutazioni_libri (
    userid text NOT NULL,
    libro_id integer NOT NULL,
    stile integer NOT NULL,
    contenuto integer NOT NULL,
    gradevolezza integer NOT NULL,
    originalita integer NOT NULL,
    edizione integer NOT NULL,
    voto_finale integer NOT NULL,
    commento character varying(256),
    CONSTRAINT valutazioni_libri_contenuto_check CHECK (((contenuto >= 1) AND (contenuto <= 5))),
    CONSTRAINT valutazioni_libri_edizione_check CHECK (((edizione >= 1) AND (edizione <= 5))),
    CONSTRAINT valutazioni_libri_gradevolezza_check CHECK (((gradevolezza >= 1) AND (gradevolezza <= 5))),
    CONSTRAINT valutazioni_libri_originalita_check CHECK (((originalita >= 1) AND (originalita <= 5))),
    CONSTRAINT valutazioni_libri_stile_check CHECK (((stile >= 1) AND (stile <= 5))),
    CONSTRAINT valutazioni_libri_voto_finale_check CHECK (((voto_finale >= 1) AND (voto_finale <= 5)))
);


ALTER TABLE br.valutazioni_libri OWNER TO br_user;

--
-- Name: books; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.books (
    id integer NOT NULL,
    title text,
    authors text,
    description text,
    category text,
    publisher text,
    price numeric,
    publish_month text,
    publish_year integer
);


ALTER TABLE public.books OWNER TO postgres;

--
-- Name: books_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.books_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.books_id_seq OWNER TO postgres;

--
-- Name: books_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.books_id_seq OWNED BY public.books.id;


--
-- Name: consigli; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.consigli (
    id integer NOT NULL,
    userid text NOT NULL,
    libro_base_id integer NOT NULL,
    libro_suggerito_id integer NOT NULL
);


ALTER TABLE public.consigli OWNER TO postgres;

--
-- Name: consigli_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.consigli_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.consigli_id_seq OWNER TO postgres;

--
-- Name: consigli_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.consigli_id_seq OWNED BY public.consigli.id;


--
-- Name: consigli_libri; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.consigli_libri (
    userid text NOT NULL,
    libro_id integer NOT NULL,
    suggerito_id integer NOT NULL
);


ALTER TABLE public.consigli_libri OWNER TO postgres;

--
-- Name: consigli_raw; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.consigli_raw (
    userid text,
    bookbaseid text,
    suggeriti text
);


ALTER TABLE public.consigli_raw OWNER TO postgres;

--
-- Name: libreria_libri; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.libreria_libri (
    libreria_id integer NOT NULL,
    libro_id integer NOT NULL
);


ALTER TABLE public.libreria_libri OWNER TO postgres;

--
-- Name: librerie; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.librerie (
    id integer NOT NULL,
    userid text NOT NULL,
    nome text NOT NULL
);


ALTER TABLE public.librerie OWNER TO postgres;

--
-- Name: librerie_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.librerie_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.librerie_id_seq OWNER TO postgres;

--
-- Name: librerie_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.librerie_id_seq OWNED BY public.librerie.id;


--
-- Name: librerie_libri; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.librerie_libri (
    libreria_id integer NOT NULL,
    libro_id integer NOT NULL
);


ALTER TABLE public.librerie_libri OWNER TO postgres;

--
-- Name: librerie_raw; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.librerie_raw (
    userid text,
    nomelibreria text,
    bookids text
);


ALTER TABLE public.librerie_raw OWNER TO postgres;

--
-- Name: libri; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.libri (
    id integer NOT NULL,
    titolo text NOT NULL,
    autori text[],
    anno integer,
    editore text,
    categoria text
);


ALTER TABLE public.libri OWNER TO postgres;

--
-- Name: libri_autori; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.libri_autori (
    libro_id integer NOT NULL,
    autore text NOT NULL
);


ALTER TABLE public.libri_autori OWNER TO postgres;

--
-- Name: libri_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.libri_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.libri_id_seq OWNER TO postgres;

--
-- Name: libri_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.libri_id_seq OWNED BY public.libri.id;


--
-- Name: libri_line; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.libri_line (
    line text
);


ALTER TABLE public.libri_line OWNER TO postgres;

--
-- Name: libri_raw; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.libri_raw (
    id text,
    titolo text,
    autori text,
    anno text,
    editore text,
    categoria text
);


ALTER TABLE public.libri_raw OWNER TO postgres;

--
-- Name: libri_staging; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.libri_staging (
    id text,
    titolo text,
    autori_raw text,
    anno text,
    editore text,
    categoria text
);


ALTER TABLE public.libri_staging OWNER TO postgres;

--
-- Name: utenti; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.utenti (
    userid text NOT NULL,
    password_hash text NOT NULL,
    nome text,
    cognome text,
    codice_fiscale text,
    email text
);


ALTER TABLE public.utenti OWNER TO postgres;

--
-- Name: utenti_raw; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.utenti_raw (
    userid text,
    password_hash text,
    nome text,
    cognome text,
    codice_fiscale text,
    email text
);


ALTER TABLE public.utenti_raw OWNER TO postgres;

--
-- Name: utenti_registrati; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.utenti_registrati (
    userid text NOT NULL,
    password_hash text NOT NULL,
    nome text NOT NULL,
    cognome text NOT NULL,
    cf text NOT NULL,
    email text NOT NULL,
    codice_fiscale text
);


ALTER TABLE public.utenti_registrati OWNER TO postgres;

--
-- Name: valutazioni; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.valutazioni (
    id integer NOT NULL,
    userid text NOT NULL,
    libro_id integer NOT NULL,
    stile integer NOT NULL,
    contenuto integer NOT NULL,
    gradevolezza integer NOT NULL,
    originalita integer NOT NULL,
    edizione integer NOT NULL,
    voto_finale integer NOT NULL,
    commento text,
    CONSTRAINT valutazioni_contenuto_check CHECK (((contenuto >= 1) AND (contenuto <= 5))),
    CONSTRAINT valutazioni_edizione_check CHECK (((edizione >= 1) AND (edizione <= 5))),
    CONSTRAINT valutazioni_gradevolezza_check CHECK (((gradevolezza >= 1) AND (gradevolezza <= 5))),
    CONSTRAINT valutazioni_originalita_check CHECK (((originalita >= 1) AND (originalita <= 5))),
    CONSTRAINT valutazioni_stile_check CHECK (((stile >= 1) AND (stile <= 5))),
    CONSTRAINT valutazioni_voto_finale_check CHECK (((voto_finale >= 1) AND (voto_finale <= 5)))
);


ALTER TABLE public.valutazioni OWNER TO postgres;

--
-- Name: valutazioni_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.valutazioni_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.valutazioni_id_seq OWNER TO postgres;

--
-- Name: valutazioni_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.valutazioni_id_seq OWNED BY public.valutazioni.id;


--
-- Name: valutazioni_libri; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.valutazioni_libri (
    userid text NOT NULL,
    libro_id integer NOT NULL,
    stile integer NOT NULL,
    contenuto integer NOT NULL,
    gradevolezza integer NOT NULL,
    originalita integer NOT NULL,
    edizione integer NOT NULL,
    voto_finale integer NOT NULL,
    commento text,
    CONSTRAINT valutazioni_libri_contenuto_check CHECK (((contenuto >= 1) AND (contenuto <= 5))),
    CONSTRAINT valutazioni_libri_edizione_check CHECK (((edizione >= 1) AND (edizione <= 5))),
    CONSTRAINT valutazioni_libri_gradevolezza_check CHECK (((gradevolezza >= 1) AND (gradevolezza <= 5))),
    CONSTRAINT valutazioni_libri_originalita_check CHECK (((originalita >= 1) AND (originalita <= 5))),
    CONSTRAINT valutazioni_libri_stile_check CHECK (((stile >= 1) AND (stile <= 5))),
    CONSTRAINT valutazioni_libri_voto_finale_check CHECK (((voto_finale >= 1) AND (voto_finale <= 5)))
);


ALTER TABLE public.valutazioni_libri OWNER TO postgres;

--
-- Name: valutazioni_raw; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.valutazioni_raw (
    userid text,
    bookid text,
    stile text,
    contenuto text,
    gradevolezza text,
    originalita text,
    edizione text,
    votofinale text,
    commento text
);


ALTER TABLE public.valutazioni_raw OWNER TO postgres;

--
-- Name: libri id; Type: DEFAULT; Schema: br; Owner: br_user
--

ALTER TABLE ONLY br.libri ALTER COLUMN id SET DEFAULT nextval('br.libri_id_seq'::regclass);


--
-- Name: books id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.books ALTER COLUMN id SET DEFAULT nextval('public.books_id_seq'::regclass);


--
-- Name: consigli id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.consigli ALTER COLUMN id SET DEFAULT nextval('public.consigli_id_seq'::regclass);


--
-- Name: librerie id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.librerie ALTER COLUMN id SET DEFAULT nextval('public.librerie_id_seq'::regclass);


--
-- Name: libri id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.libri ALTER COLUMN id SET DEFAULT nextval('public.libri_id_seq'::regclass);


--
-- Name: valutazioni id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.valutazioni ALTER COLUMN id SET DEFAULT nextval('public.valutazioni_id_seq'::regclass);


--
-- Name: consigli_libri consigli_libri_pkey; Type: CONSTRAINT; Schema: br; Owner: br_user
--

ALTER TABLE ONLY br.consigli_libri
    ADD CONSTRAINT consigli_libri_pkey PRIMARY KEY (userid, libro_id, suggerito_id);


--
-- Name: librerie_libri librerie_libri_pkey; Type: CONSTRAINT; Schema: br; Owner: br_user
--

ALTER TABLE ONLY br.librerie_libri
    ADD CONSTRAINT librerie_libri_pkey PRIMARY KEY (userid, nome, libro_id);


--
-- Name: librerie librerie_pkey; Type: CONSTRAINT; Schema: br; Owner: br_user
--

ALTER TABLE ONLY br.librerie
    ADD CONSTRAINT librerie_pkey PRIMARY KEY (userid, nome);


--
-- Name: libri_autori libri_autori_pkey; Type: CONSTRAINT; Schema: br; Owner: br_user
--

ALTER TABLE ONLY br.libri_autori
    ADD CONSTRAINT libri_autori_pkey PRIMARY KEY (libro_id, autore);


--
-- Name: libri libri_pkey; Type: CONSTRAINT; Schema: br; Owner: br_user
--

ALTER TABLE ONLY br.libri
    ADD CONSTRAINT libri_pkey PRIMARY KEY (id);


--
-- Name: utenti_registrati uq_cf; Type: CONSTRAINT; Schema: br; Owner: br_user
--

ALTER TABLE ONLY br.utenti_registrati
    ADD CONSTRAINT uq_cf UNIQUE (codice_fiscale);


--
-- Name: utenti_registrati uq_email; Type: CONSTRAINT; Schema: br; Owner: br_user
--

ALTER TABLE ONLY br.utenti_registrati
    ADD CONSTRAINT uq_email UNIQUE (email);


--
-- Name: utenti_registrati utenti_registrati_pkey; Type: CONSTRAINT; Schema: br; Owner: br_user
--

ALTER TABLE ONLY br.utenti_registrati
    ADD CONSTRAINT utenti_registrati_pkey PRIMARY KEY (userid);


--
-- Name: valutazioni_libri valutazioni_libri_pkey; Type: CONSTRAINT; Schema: br; Owner: br_user
--

ALTER TABLE ONLY br.valutazioni_libri
    ADD CONSTRAINT valutazioni_libri_pkey PRIMARY KEY (userid, libro_id);


--
-- Name: books books_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.books
    ADD CONSTRAINT books_pkey PRIMARY KEY (id);


--
-- Name: consigli_libri consigli_libri_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.consigli_libri
    ADD CONSTRAINT consigli_libri_pkey PRIMARY KEY (userid, libro_id, suggerito_id);


--
-- Name: consigli consigli_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.consigli
    ADD CONSTRAINT consigli_pkey PRIMARY KEY (id);


--
-- Name: consigli consigli_userid_libro_base_id_libro_suggerito_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.consigli
    ADD CONSTRAINT consigli_userid_libro_base_id_libro_suggerito_id_key UNIQUE (userid, libro_base_id, libro_suggerito_id);


--
-- Name: libreria_libri libreria_libri_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.libreria_libri
    ADD CONSTRAINT libreria_libri_pkey PRIMARY KEY (libreria_id, libro_id);


--
-- Name: librerie_libri librerie_libri_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.librerie_libri
    ADD CONSTRAINT librerie_libri_pkey PRIMARY KEY (libreria_id, libro_id);


--
-- Name: librerie librerie_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.librerie
    ADD CONSTRAINT librerie_pkey PRIMARY KEY (id);


--
-- Name: librerie librerie_userid_nome_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.librerie
    ADD CONSTRAINT librerie_userid_nome_key UNIQUE (userid, nome);


--
-- Name: libri_autori libri_autori_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.libri_autori
    ADD CONSTRAINT libri_autori_pkey PRIMARY KEY (libro_id, autore);


--
-- Name: libri libri_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.libri
    ADD CONSTRAINT libri_pkey PRIMARY KEY (id);


--
-- Name: utenti utenti_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.utenti
    ADD CONSTRAINT utenti_pkey PRIMARY KEY (userid);


--
-- Name: utenti_registrati utenti_registrati_cf_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.utenti_registrati
    ADD CONSTRAINT utenti_registrati_cf_key UNIQUE (cf);


--
-- Name: utenti_registrati utenti_registrati_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.utenti_registrati
    ADD CONSTRAINT utenti_registrati_email_key UNIQUE (email);


--
-- Name: utenti_registrati utenti_registrati_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.utenti_registrati
    ADD CONSTRAINT utenti_registrati_pkey PRIMARY KEY (userid);


--
-- Name: valutazioni_libri valutazioni_libri_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.valutazioni_libri
    ADD CONSTRAINT valutazioni_libri_pkey PRIMARY KEY (userid, libro_id);


--
-- Name: valutazioni valutazioni_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.valutazioni
    ADD CONSTRAINT valutazioni_pkey PRIMARY KEY (id);


--
-- Name: valutazioni valutazioni_userid_libro_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.valutazioni
    ADD CONSTRAINT valutazioni_userid_libro_id_key UNIQUE (userid, libro_id);


--
-- Name: ix_autore_lower; Type: INDEX; Schema: br; Owner: br_user
--

CREATE INDEX ix_autore_lower ON br.libri_autori USING btree (lower(autore));


--
-- Name: ix_autore_trgm; Type: INDEX; Schema: br; Owner: br_user
--

CREATE INDEX ix_autore_trgm ON br.libri_autori USING gin (autore public.gin_trgm_ops);


--
-- Name: ix_consigli_libro; Type: INDEX; Schema: br; Owner: br_user
--

CREATE INDEX ix_consigli_libro ON br.consigli_libri USING btree (libro_id);


--
-- Name: ix_consigli_user; Type: INDEX; Schema: br; Owner: br_user
--

CREATE INDEX ix_consigli_user ON br.consigli_libri USING btree (userid);


--
-- Name: ix_librerie_libri_libro; Type: INDEX; Schema: br; Owner: br_user
--

CREATE INDEX ix_librerie_libri_libro ON br.librerie_libri USING btree (libro_id);


--
-- Name: ix_librerie_libro; Type: INDEX; Schema: br; Owner: br_user
--

CREATE INDEX ix_librerie_libro ON br.librerie_libri USING btree (libro_id);


--
-- Name: ix_librerie_user; Type: INDEX; Schema: br; Owner: br_user
--

CREATE INDEX ix_librerie_user ON br.librerie USING btree (userid);


--
-- Name: ix_libri_autori_autore_trgm; Type: INDEX; Schema: br; Owner: br_user
--

CREATE INDEX ix_libri_autori_autore_trgm ON br.libri_autori USING gin (autore public.gin_trgm_ops);


--
-- Name: ix_libri_titolo_lower; Type: INDEX; Schema: br; Owner: br_user
--

CREATE INDEX ix_libri_titolo_lower ON br.libri USING btree (lower(titolo));


--
-- Name: ix_libri_titolo_trgm; Type: INDEX; Schema: br; Owner: br_user
--

CREATE INDEX ix_libri_titolo_trgm ON br.libri USING gin (titolo public.gin_trgm_ops);


--
-- Name: ix_valutazioni_libro; Type: INDEX; Schema: br; Owner: br_user
--

CREATE INDEX ix_valutazioni_libro ON br.valutazioni_libri USING btree (libro_id);


--
-- Name: ix_valutazioni_user; Type: INDEX; Schema: br; Owner: br_user
--

CREATE INDEX ix_valutazioni_user ON br.valutazioni_libri USING btree (userid);


--
-- Name: idx_libri_autori_gin; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_libri_autori_gin ON public.libri USING gin (autori);


--
-- Name: idx_libri_titolo; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_libri_titolo ON public.libri USING gin (to_tsvector('simple'::regconfig, titolo));


--
-- Name: consigli_libri consigli_libri_libro_id_fkey; Type: FK CONSTRAINT; Schema: br; Owner: br_user
--

ALTER TABLE ONLY br.consigli_libri
    ADD CONSTRAINT consigli_libri_libro_id_fkey FOREIGN KEY (libro_id) REFERENCES br.libri(id) ON DELETE CASCADE;


--
-- Name: consigli_libri consigli_libri_suggerito_id_fkey; Type: FK CONSTRAINT; Schema: br; Owner: br_user
--

ALTER TABLE ONLY br.consigli_libri
    ADD CONSTRAINT consigli_libri_suggerito_id_fkey FOREIGN KEY (suggerito_id) REFERENCES br.libri(id) ON DELETE CASCADE;


--
-- Name: consigli_libri consigli_libri_userid_fkey; Type: FK CONSTRAINT; Schema: br; Owner: br_user
--

ALTER TABLE ONLY br.consigli_libri
    ADD CONSTRAINT consigli_libri_userid_fkey FOREIGN KEY (userid) REFERENCES br.utenti_registrati(userid) ON DELETE CASCADE;


--
-- Name: librerie_libri librerie_libri_libro_id_fkey; Type: FK CONSTRAINT; Schema: br; Owner: br_user
--

ALTER TABLE ONLY br.librerie_libri
    ADD CONSTRAINT librerie_libri_libro_id_fkey FOREIGN KEY (libro_id) REFERENCES br.libri(id) ON DELETE CASCADE;


--
-- Name: librerie_libri librerie_libri_userid_nome_fkey; Type: FK CONSTRAINT; Schema: br; Owner: br_user
--

ALTER TABLE ONLY br.librerie_libri
    ADD CONSTRAINT librerie_libri_userid_nome_fkey FOREIGN KEY (userid, nome) REFERENCES br.librerie(userid, nome) ON DELETE CASCADE;


--
-- Name: librerie librerie_userid_fkey; Type: FK CONSTRAINT; Schema: br; Owner: br_user
--

ALTER TABLE ONLY br.librerie
    ADD CONSTRAINT librerie_userid_fkey FOREIGN KEY (userid) REFERENCES br.utenti_registrati(userid) ON DELETE CASCADE;


--
-- Name: libri_autori libri_autori_libro_id_fkey; Type: FK CONSTRAINT; Schema: br; Owner: br_user
--

ALTER TABLE ONLY br.libri_autori
    ADD CONSTRAINT libri_autori_libro_id_fkey FOREIGN KEY (libro_id) REFERENCES br.libri(id) ON DELETE CASCADE;


--
-- Name: valutazioni_libri valutazioni_libri_libro_id_fkey; Type: FK CONSTRAINT; Schema: br; Owner: br_user
--

ALTER TABLE ONLY br.valutazioni_libri
    ADD CONSTRAINT valutazioni_libri_libro_id_fkey FOREIGN KEY (libro_id) REFERENCES br.libri(id) ON DELETE CASCADE;


--
-- Name: valutazioni_libri valutazioni_libri_userid_fkey; Type: FK CONSTRAINT; Schema: br; Owner: br_user
--

ALTER TABLE ONLY br.valutazioni_libri
    ADD CONSTRAINT valutazioni_libri_userid_fkey FOREIGN KEY (userid) REFERENCES br.utenti_registrati(userid) ON DELETE CASCADE;


--
-- Name: consigli_libri consigli_libri_libro_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.consigli_libri
    ADD CONSTRAINT consigli_libri_libro_id_fkey FOREIGN KEY (libro_id) REFERENCES public.libri(id) ON DELETE RESTRICT;


--
-- Name: consigli_libri consigli_libri_suggerito_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.consigli_libri
    ADD CONSTRAINT consigli_libri_suggerito_id_fkey FOREIGN KEY (suggerito_id) REFERENCES public.libri(id) ON DELETE RESTRICT;


--
-- Name: consigli_libri consigli_libri_userid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.consigli_libri
    ADD CONSTRAINT consigli_libri_userid_fkey FOREIGN KEY (userid) REFERENCES public.utenti_registrati(userid) ON DELETE CASCADE;


--
-- Name: libreria_libri libreria_libri_libreria_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.libreria_libri
    ADD CONSTRAINT libreria_libri_libreria_id_fkey FOREIGN KEY (libreria_id) REFERENCES public.librerie(id) ON DELETE CASCADE;


--
-- Name: librerie_libri librerie_libri_libreria_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.librerie_libri
    ADD CONSTRAINT librerie_libri_libreria_id_fkey FOREIGN KEY (libreria_id) REFERENCES public.librerie(id) ON DELETE CASCADE;


--
-- Name: librerie_libri librerie_libri_libro_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.librerie_libri
    ADD CONSTRAINT librerie_libri_libro_id_fkey FOREIGN KEY (libro_id) REFERENCES public.libri(id) ON DELETE RESTRICT;


--
-- Name: libri_autori libri_autori_libro_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.libri_autori
    ADD CONSTRAINT libri_autori_libro_id_fkey FOREIGN KEY (libro_id) REFERENCES public.libri(id) ON DELETE CASCADE;


--
-- Name: valutazioni_libri valutazioni_libri_libro_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.valutazioni_libri
    ADD CONSTRAINT valutazioni_libri_libro_id_fkey FOREIGN KEY (libro_id) REFERENCES public.libri(id) ON DELETE RESTRICT;


--
-- Name: valutazioni_libri valutazioni_libri_userid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.valutazioni_libri
    ADD CONSTRAINT valutazioni_libri_userid_fkey FOREIGN KEY (userid) REFERENCES public.utenti_registrati(userid) ON DELETE CASCADE;


--
-- Name: TABLE books; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.books TO br_user;


--
-- Name: SEQUENCE books_id_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.books_id_seq TO br_user;


--
-- Name: TABLE consigli; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.consigli TO br_user;


--
-- Name: SEQUENCE consigli_id_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.consigli_id_seq TO br_user;


--
-- Name: TABLE consigli_libri; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.consigli_libri TO br_user;


--
-- Name: TABLE consigli_raw; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.consigli_raw TO br_user;


--
-- Name: TABLE libreria_libri; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.libreria_libri TO br_user;


--
-- Name: TABLE librerie; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.librerie TO br_user;


--
-- Name: SEQUENCE librerie_id_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.librerie_id_seq TO br_user;


--
-- Name: TABLE librerie_libri; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.librerie_libri TO br_user;


--
-- Name: TABLE librerie_raw; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.librerie_raw TO br_user;


--
-- Name: TABLE libri; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.libri TO br_user;


--
-- Name: TABLE libri_autori; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.libri_autori TO br_user;


--
-- Name: SEQUENCE libri_id_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.libri_id_seq TO br_user;


--
-- Name: TABLE libri_line; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.libri_line TO br_user;


--
-- Name: TABLE libri_raw; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.libri_raw TO br_user;


--
-- Name: TABLE libri_staging; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.libri_staging TO br_user;


--
-- Name: TABLE utenti; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.utenti TO br_user;


--
-- Name: TABLE utenti_raw; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.utenti_raw TO br_user;


--
-- Name: TABLE utenti_registrati; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.utenti_registrati TO br_user;


--
-- Name: TABLE valutazioni; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.valutazioni TO br_user;


--
-- Name: SEQUENCE valutazioni_id_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE public.valutazioni_id_seq TO br_user;


--
-- Name: TABLE valutazioni_libri; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.valutazioni_libri TO br_user;


--
-- Name: TABLE valutazioni_raw; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.valutazioni_raw TO br_user;


--
-- Name: DEFAULT PRIVILEGES FOR SEQUENCES; Type: DEFAULT ACL; Schema: public; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON SEQUENCES TO br_user;


--
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: public; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON TABLES TO br_user;


--
-- PostgreSQL database dump complete
--

\unrestrict 0lMWG6b1mAwffy9Jxj9gheFjCC81RcuHIlPwec8lGrbJ8poBFXuhgzTBGh92bni

