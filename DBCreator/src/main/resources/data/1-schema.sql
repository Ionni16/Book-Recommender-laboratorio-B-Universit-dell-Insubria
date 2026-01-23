--
-- PostgreSQL database dump
--


-- Dumped from database version 16.11
-- Dumped by pg_dump version 16.11
SET search_path TO br, public;
SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: br; Type: SCHEMA; Schema: -; Owner: br_user
--

CREATE SCHEMA IF NOT EXISTS br;





SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: consigli_libri; Type: TABLE; Schema: br; Owner: br_user
--

CREATE TABLE IF NOT EXISTS br.consigli_libri (
    userid text NOT NULL,
    libro_id integer NOT NULL,
    suggerito_id integer NOT NULL
);


--
-- Name: librerie; Type: TABLE; Schema: br; Owner: br_user
--

CREATE TABLE IF NOT EXISTS br.librerie (
    userid text NOT NULL,
    nome text NOT NULL
);


--
-- Name: librerie_libri; Type: TABLE; Schema: br; Owner: br_user
--

CREATE TABLE IF NOT EXISTS br.librerie_libri (
    userid text NOT NULL,
    nome text NOT NULL,
    libro_id integer NOT NULL
);



--
-- Name: libri; Type: TABLE; Schema: br; Owner: br_user
--

CREATE TABLE IF NOT EXISTS br.libri (
    id integer NOT NULL,
    titolo text NOT NULL,
    anno integer,
    editore text,
    categoria text
);



--
-- Name: libri_autori; Type: TABLE; Schema: br; Owner: br_user
--

CREATE TABLE IF NOT EXISTS br.libri_autori (
    libro_id integer NOT NULL,
    autore text NOT NULL
);



--
-- Name: libri_id_seq; Type: SEQUENCE; Schema: br; Owner: br_user
--

CREATE SEQUENCE IF NOT EXISTS br.libri_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE br.libri_id_seq OWNED BY br.libri.id;
ALTER TABLE ONLY br.libri ALTER COLUMN id SET DEFAULT nextval('br.libri_id_seq'::regclass);


--
-- Name: utenti_registrati; Type: TABLE; Schema: br; Owner: br_user
--

CREATE TABLE IF NOT EXISTS br.utenti_registrati (
    userid text NOT NULL,
    password_hash text NOT NULL,
    nome text NOT NULL,
    cognome text NOT NULL,
    codice_fiscale text NOT NULL,
    email text NOT NULL,
    CONSTRAINT chk_pwdhash_len CHECK ((length(password_hash) = 64))
);


--
-- Name: valutazioni_libri; Type: TABLE; Schema: br; Owner: br_user
--

CREATE TABLE IF NOT EXISTS br.valutazioni_libri (
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


